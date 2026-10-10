import { ChevronRight, Home } from 'lucide-react';
import { Link } from 'react-router-dom';

interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
}

export default function Breadcrumb({ items }: BreadcrumbProps) {
  return (
    <div className="flex items-center text-xs text-slate-500 mb-6 font-medium tracking-wide">
      <Link to="/" className="hover:text-blue-600 transition flex items-center">
        <Home className="h-3.5 w-3.5 mr-1.5" />
        Home
      </Link>
      
      {items.map((item, index) => (
        <div key={item.label} className="flex items-center">
          <ChevronRight className="h-3.5 w-3.5 mx-2 text-slate-300" />
          {item.href ? (
            <Link to={item.href} className="hover:text-blue-600 transition">
              {item.label}
            </Link>
          ) : (
            <span className="text-slate-800 font-semibold">{item.label}</span>
          )}
        </div>
      ))}
    </div>
  );
}
