import ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';

const formatHHMM = (seconds: number) => {
  if (!seconds) return '-';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
};

const formatText = (seconds: number) => {
  if (!seconds) return '0h 00m';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m.toString().padStart(2, '0')}m`;
};

const THEME_COLOR = '107C41'; // Excel Green Background
const LIGHT_GREEN = 'E8F5E9';
const WHITE = 'FFFFFF';
const TEXT_DARK = '000000';
const BORDER_COLOR = 'CCCCCC';

export const exportTimesheetToExcel = async (
  data: any, 
  viewMode: 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'
) => {
  if (!data) return;

  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'EMS System';
  workbook.created = new Date();

  let targetHours = ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00'];
  if (viewMode === 'DAY' && data.hourly && data.hourly.length > 0) {
    const hours = data.hourly.map((h: any) => parseInt(h.hour.split(':')[0], 10));
    const minHour = Math.min(...hours);
    const maxHour = Math.max(...hours);
    targetHours = [];
    for (let i = minHour; i <= maxHour; i++) targetHours.push(`${i.toString().padStart(2, '0')}:00`);
  }

  const lastColNum = viewMode === 'DAY' ? (5 + targetHours.length) : 10;
  
  const getColLetter = (colNum: number) => {
    let letter = '';
    while (colNum > 0) {
      let mod = (colNum - 1) % 26;
      letter = String.fromCharCode(65 + mod) + letter;
      colNum = Math.floor((colNum - mod) / 26);
    }
    return letter;
  };

  const lastColLetter = getColLetter(lastColNum);
  const infoLabelCol = getColLetter(lastColNum - 2);
  const infoColonCol = getColLetter(lastColNum - 1);
  const infoValueCol = getColLetter(lastColNum);

  let modeName = viewMode.charAt(0) + viewMode.slice(1).toLowerCase();
  if (viewMode === 'DAY') modeName = 'Daily';
  else if (viewMode === 'WEEK') modeName = 'Weekly';
  else if (viewMode === 'MONTH') modeName = 'Monthly';
  else if (viewMode === 'YEAR') modeName = 'Yearly';
  
  const sheetName = `${modeName} Report`;
  const sheet = workbook.addWorksheet(sheetName, {
    views: [{ showGridLines: false }]
  });

  const setSolidFill = (cell: ExcelJS.Cell, color: string) => {
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: color } };
  };

  const setFont = (cell: ExcelJS.Cell, bold = false, color = TEXT_DARK, size = 11) => {
    cell.font = { name: 'Calibri', bold, color: { argb: color }, size };
  };

  const setBorders = (cell: ExcelJS.Cell) => {
    cell.border = {
      top: { style: 'thin', color: { argb: BORDER_COLOR } },
      left: { style: 'thin', color: { argb: BORDER_COLOR } },
      bottom: { style: 'thin', color: { argb: BORDER_COLOR } },
      right: { style: 'thin', color: { argb: BORDER_COLOR } }
    };
  };

  // 1. Top Header
  sheet.mergeCells(`A1:${lastColLetter}1`);
  const titleCell = sheet.getCell('A1');
  titleCell.value = `Employee Timesheet - ${modeName} Report`;
  setFont(titleCell, true, WHITE, 16);
  titleCell.alignment = { horizontal: 'center', vertical: 'middle' };
  setSolidFill(titleCell, THEME_COLOR);
  sheet.getRow(1).height = 30;

  // 2. Company & Report Info
  sheet.mergeCells('A3:C3');
  const logoCell = sheet.getCell('A3');
  logoCell.value = 'EMS Employee Monitoring System';
  setFont(logoCell, true, THEME_COLOR, 14);

  const now = new Date();
  const reportInfo = [
    { label: 'Generated On:', value: now.toLocaleString('en-IN'), row: 2 },
    { label: 'Period:', value: data.startDateLabel || '-', row: 3 }
  ];

  reportInfo.forEach(info => {
    sheet.mergeCells(`${getColLetter(lastColNum - 3)}${info.row}:${getColLetter(lastColNum - 2)}${info.row}`);
    const lblCell = sheet.getCell(`${getColLetter(lastColNum - 3)}${info.row}`);
    lblCell.value = info.label;
    lblCell.alignment = { horizontal: 'right' };
    setFont(lblCell, false, '666666', 10);

    sheet.mergeCells(`${getColLetter(lastColNum - 1)}${info.row}:${lastColLetter}${info.row}`);
    const valCell = sheet.getCell(`${getColLetter(lastColNum - 1)}${info.row}`);
    valCell.value = info.value;
    valCell.alignment = { horizontal: 'left' };
    setFont(valCell, false, '666666', 10);
  });

  // 3. Employee Info
  const empInfo = [
    { label: 'Employee Code:', value: data.employeeCode, row: 5 },
    { label: 'Employee Name:', value: data.employeeName, row: 6 },
    { label: 'Department:', value: data.departmentName || '-', row: 7 },
    { label: 'Team:', value: '-', row: 8 }
  ];

  empInfo.forEach(info => {
    const lblCell = sheet.getCell(`B${info.row}`);
    lblCell.value = info.label;
    setFont(lblCell, true);
    
    sheet.mergeCells(`C${info.row}:E${info.row}`);
    const valCell = sheet.getCell(`C${info.row}`);
    valCell.value = info.value;
    valCell.alignment = { horizontal: 'left' };
  });

  const rightInfo = [
    { label: 'Total Work Time:', value: formatText(data.summary?.workSeconds), row: 5 },
    { label: 'Total Idle Time:', value: formatText(data.summary?.idleSeconds), row: 6 },
    { label: 'Total Break Time:', value: formatText(data.summary?.breakSeconds), row: 7 },
    { label: 'Overall Efficiency:', value: `${data.summary?.efficiency || 0}%`, row: 8 }
  ];

  rightInfo.forEach(info => {
    sheet.mergeCells(`${getColLetter(lastColNum - 3)}${info.row}:${getColLetter(lastColNum - 2)}${info.row}`);
    const lblCell = sheet.getCell(`${getColLetter(lastColNum - 3)}${info.row}`);
    lblCell.value = info.label;
    lblCell.alignment = { horizontal: 'right' };
    setFont(lblCell, true);

    sheet.mergeCells(`${getColLetter(lastColNum - 1)}${info.row}:${lastColLetter}${info.row}`);
    const valCell = sheet.getCell(`${getColLetter(lastColNum - 1)}${info.row}`);
    valCell.value = info.value;
    valCell.alignment = { horizontal: 'left' };
    setFont(valCell, true);
  });

  let currentRow = 11;

  // 4. Data Tables
  if (viewMode === 'DAY') {
    const renderDayTable = (type: 'WORK'|'IDLE'|'BREAK'|'LUNCH', title: string) => {
      if (!data.hourly) return;

      const displayHours = targetHours.map(h => {
        let hour = parseInt(h.split(':')[0], 10);
        hour = hour > 12 ? hour - 12 : hour;
        return `${hour.toString().padStart(2, '0')}:00`;
      });

      const getHourSecs = (h: string) => {
        const row = data.hourly?.find((r: any) => r.hour === h);
        if (!row) return 0;
        if (type === 'WORK') return row.workSeconds;
        if (type === 'BREAK') return row.breakSeconds;
        if (type === 'LUNCH') return row.lunchSeconds;
        if (type === 'IDLE') return row.idleSeconds;
        return 0;
      };

      const rowData = targetHours.map(h => getHourSecs(h));
      const totalSecs = rowData.reduce((a, b) => a + b, 0);

      let client = 'System', project = 'Idle', task = 'No Activity';
      if (type === 'WORK') { client = 'ABC Technologies'; project = 'Web Application'; task = 'Development'; }
      if (type === 'BREAK') { client = 'System'; project = 'Break'; task = 'Tea Break'; }
      if (type === 'LUNCH') { client = 'Office'; project = 'Lunch'; task = 'Lunch Break'; }

      sheet.mergeCells(`B${currentRow}:${lastColLetter}${currentRow}`);
      const titleCell = sheet.getCell(`B${currentRow}`);
      titleCell.value = title;
      setFont(titleCell, true, THEME_COLOR, 12);
      currentRow++;

      const headers = ['Client Name', 'Project Name', 'Task Name', ...displayHours, 'Total (HH:MM)'];
      headers.forEach((h, i) => {
        const cell = sheet.getCell(currentRow, 2 + i);
        cell.value = h;
        setFont(cell, true);
        setSolidFill(cell, LIGHT_GREEN);
        setBorders(cell);
        cell.alignment = { horizontal: 'center', vertical: 'middle' };
      });
      sheet.getRow(currentRow).height = 20;
      currentRow++;

      const dataRow = [client, project, task, ...rowData.map(s => formatHHMM(s)), formatHHMM(totalSecs)];
      dataRow.forEach((val, i) => {
        const cell = sheet.getCell(currentRow, 2 + i);
        cell.value = val;
        setBorders(cell);
        cell.alignment = { horizontal: 'center' };
      });
      currentRow += 2; 
    };

    renderDayTable('WORK', 'Work Time');
    renderDayTable('BREAK', 'Break Time');
    renderDayTable('LUNCH', 'Lunch Time');
    renderDayTable('IDLE', 'Idle Time');

  } else {
    const headers = ['Date', 'Day', 'Work Hours', 'Idle Time', 'Break Time', 'Lunch Time', 'Total Logged In', 'Efficiency', 'Status'];
    
    headers.forEach((h, i) => {
      const cell = sheet.getCell(currentRow, 2 + i);
      cell.value = h;
      setFont(cell, true);
      setSolidFill(cell, LIGHT_GREEN);
      setBorders(cell);
      cell.alignment = { horizontal: 'center', vertical: 'middle' };
    });
    sheet.getRow(currentRow).height = 25;
    currentRow++;

    if (data.rows && data.rows.length > 0) {
      data.rows.forEach((r: any) => {
        const rowData = [
          r.label || r.date,
          r.dayName || '-',
          formatHHMM(r.workSeconds),
          formatHHMM(r.idleSeconds),
          formatHHMM(r.breakSeconds),
          formatHHMM(r.lunchSeconds),
          formatHHMM(r.totalSeconds),
          `${r.efficiency || 0}%`,
          r.workSeconds > 0 ? 'Present' : (['Sat', 'Sun'].includes(r.dayName?.substring(0,3)) ? 'Weekend' : 'Absent')
        ];

        rowData.forEach((val, i) => {
          const cell = sheet.getCell(currentRow, 2 + i);
          cell.value = val;
          setBorders(cell);
          cell.alignment = { horizontal: 'center' };
          
          if (i === 8) {
            if (val === 'Present') setFont(cell, true, '2E7D32'); 
            else if (val === 'Absent') setFont(cell, true, 'C62828'); 
            else if (val === 'Weekend') setFont(cell, true, 'E65100'); 
          }
        });
        currentRow++;
      });

      const totalRow = [
        'TOTAL', '-',
        formatHHMM(data.summary?.workSeconds),
        formatHHMM(data.summary?.idleSeconds),
        formatHHMM(data.summary?.breakSeconds),
        formatHHMM(data.summary?.lunchSeconds),
        formatHHMM(data.summary?.totalSeconds),
        `${data.summary?.efficiency || 0}%`,
        '-'
      ];
      
      totalRow.forEach((val, i) => {
        const cell = sheet.getCell(currentRow, 2 + i);
        cell.value = val;
        setFont(cell, true);
        setSolidFill(cell, LIGHT_GREEN);
        setBorders(cell);
        cell.alignment = { horizontal: 'center' };
      });
    } else {
      const cell = sheet.getCell(currentRow, 2);
      cell.value = 'No data available for this period';
      sheet.mergeCells(`B${currentRow}:J${currentRow}`);
      cell.alignment = { horizontal: 'center' };
    }
  }

  sheet.getColumn(1).width = 3;  
  sheet.getColumn(2).width = 20; 
  sheet.getColumn(3).width = 15; 
  sheet.getColumn(4).width = 20; 
  
  if (viewMode === 'DAY') {
    for (let i = 5; i <= lastColNum - 1; i++) {
      sheet.getColumn(i).width = 10;
    }
    sheet.getColumn(lastColNum).width = 15;
  } else {
    for (let i = 5; i <= lastColNum; i++) {
      sheet.getColumn(i).width = 15;
    }
  }

  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
  saveAs(blob, `Timesheet_${data.employeeName}_${viewMode}.xlsx`);
};
