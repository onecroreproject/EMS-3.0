import { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check } from 'lucide-react';

interface CustomSelectProps {
  options: { label: string; value: string }[];
  value: string;
  onChange: (val: string) => void;
  placeholder?: string;
  icon?: React.ReactNode;
  themeColor?: string; // e.g. 'blue', 'orange', 'purple', 'green'
  size?: 'sm' | 'md' | 'lg';
}

export default function CustomSelect({ options, value, onChange, placeholder = "Select...", icon, themeColor = "blue", size = "sm" }: CustomSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const selectedOption = options.find(o => o.value === value);

  // Minimalist Theme mappings for borders and selection backgrounds
  const themeRings: Record<string, string> = {
    orange: 'ring-4 ring-[#F96D3E]/20 border-[#F96D3E]',
    blue: 'ring-4 ring-blue-500/20 border-blue-500',
    purple: 'ring-4 ring-purple-500/20 border-purple-500',
    green: 'ring-4 ring-green-500/20 border-green-500',
  };

  const themeSelected: Record<string, string> = {
    orange: 'bg-orange-50 text-[#F96D3E] dark:bg-orange-500/20 dark:text-orange-400',
    blue: 'bg-blue-50 text-blue-700 dark:bg-blue-500/20 dark:text-blue-400',
    purple: 'bg-purple-50 text-purple-700 dark:bg-purple-500/20 dark:text-purple-400',
    green: 'bg-green-50 text-green-700 dark:bg-green-500/20 dark:text-green-400',
  };

  const ringClass = themeRings[themeColor] || themeRings.blue;
  const selectedClass = themeSelected[themeColor] || themeSelected.blue;

  const sizeClasses = {
    sm: 'py-1.5 text-xs',
    md: 'py-2 text-sm',
    lg: 'py-2.5 text-sm'
  };

  return (
    <div className="relative w-full" ref={ref}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={`w-full flex items-center justify-between ${icon ? 'pl-9' : 'pl-3.5'} pr-3 ${sizeClasses[size]} bg-white dark:bg-slate-900/70 border border-slate-200 dark:border-slate-700/80 transition-all font-semibold text-slate-700 dark:text-slate-200 rounded-xl focus:outline-none shadow-xs hover:border-slate-300 dark:hover:border-slate-600 ${isOpen ? ringClass : ''}`}
      >
        {icon && (
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400 dark:text-slate-500">
            {icon}
          </div>
        )}
        
        <span className={selectedOption && value !== '' ? "text-slate-800 dark:text-slate-100 font-medium" : "text-slate-400 dark:text-slate-500 font-normal"}>
          {selectedOption ? selectedOption.label : placeholder}
        </span>
        
        <ChevronDown className={`h-4 w-4 text-slate-400 dark:text-slate-500 transition-transform duration-200 ml-2 ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {isOpen && (
        <div className="absolute z-50 w-full mt-1.5 bg-white dark:bg-[#1E293B] border border-slate-100 dark:border-slate-700/80 rounded-xl shadow-2xl py-1.5 transform opacity-100 scale-100 transition-all origin-top-right overflow-hidden">
          <div className="max-h-60 overflow-y-auto custom-scrollbar">
            {options.map((opt) => {
              const isSelected = opt.value === value;
              return (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => {
                    onChange(opt.value);
                    setIsOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3.5 py-2 text-xs transition-colors cursor-pointer
                    ${isSelected ? selectedClass : 'text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800/80'}
                  `}
                >
                  <span className={isSelected ? 'font-bold' : 'font-medium'}>{opt.label}</span>
                  {isSelected && <Check className="h-3.5 w-3.5" />}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
