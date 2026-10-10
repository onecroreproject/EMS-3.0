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
    orange: 'bg-orange-50 text-[#F96D3E]',
    blue: 'bg-blue-50 text-blue-700',
    purple: 'bg-purple-50 text-purple-700',
    green: 'bg-green-50 text-green-700',
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
        className={`w-full flex items-center justify-between ${icon ? 'pl-9' : 'pl-3'} pr-2 ${sizeClasses[size]} bg-white border transition-all font-semibold text-slate-700 rounded-lg focus:outline-none focus:ring-0 shadow-sm hover:border-slate-300 ${isOpen ? ringClass : 'border-slate-200'}`}
      >
        {icon && (
          <div className="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-slate-400">
            {icon}
          </div>
        )}
        
        <span className={selectedOption && value !== '' ? "text-slate-800" : "text-slate-500"}>
          {selectedOption ? selectedOption.label : placeholder}
        </span>
        
        <ChevronDown className={`h-3.5 w-3.5 text-slate-400 transition-transform duration-200 ml-2 ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {isOpen && (
        <div className="absolute z-50 w-full mt-1.5 bg-white border border-slate-100 rounded-xl shadow-[0_10px_40px_-10px_rgba(0,0,0,0.1)] py-1.5 transform opacity-100 scale-100 transition-all origin-top-right overflow-hidden">
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
                  className={`w-full flex items-center justify-between px-3 py-1.5 text-xs transition-colors
                    ${isSelected ? selectedClass : 'text-slate-600 hover:bg-slate-50'}
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
