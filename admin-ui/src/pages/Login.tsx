import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, Lock, ShieldCheck, QrCode, ArrowRight, CheckCircle2, ChevronLeft, Activity, Users, Clock } from 'lucide-react';

export default function Login() {
  const navigate = useNavigate();
  const [step, setStep] = useState<1 | 2>(1);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [mfaCode, setMfaCode] = useState('');
  const [error, setError] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  
  const [isFirstTimeSetup, setIsFirstTimeSetup] = useState(false);
  const [qrUrl, setQrUrl] = useState('');
  const [mfaSecret, setMfaSecret] = useState('');

  // Mouse tracking for interactive left panel
  const leftPanelRef = useRef<HTMLDivElement>(null);
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  useEffect(() => {
    const savedEmail = localStorage.getItem('rememberedEmail');
    if (savedEmail) {
      setEmail(savedEmail);
      setRememberMe(true);
    }
  }, []);

  const handleMouseMove = (e: React.MouseEvent) => {
    if (leftPanelRef.current) {
      const rect = leftPanelRef.current.getBoundingClientRect();
      setMousePos({
        x: e.clientX - rect.left,
        y: e.clientY - rect.top,
      });
    }
  };

  const handleCredentialsSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (rememberMe) {
      localStorage.setItem('rememberedEmail', email);
    } else {
      localStorage.removeItem('rememberedEmail');
    }

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      
      if (!response.ok) {
        throw new Error('Invalid email or password');
      }
      
      const data = await response.json();
      
      if (data.mfaRequired) {
        setIsFirstTimeSetup(data.setupRequired);
        if (data.setupRequired) {
          setQrUrl(data.qrCode);
          setMfaSecret(data.secret);
        }
        setStep(2);
      }
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleMfaSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const response = await fetch('/api/auth/verify-mfa', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, code: mfaCode })
      });
      
      if (!response.ok) {
        throw new Error('Invalid verification code');
      }
      
      const data = await response.json();
      
      if (rememberMe) {
        localStorage.setItem('adminToken', data.token);
      } else {
        sessionStorage.setItem('adminToken', data.token);
      }
      
      navigate('/');
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="min-h-screen bg-white flex">
      
      {/* Dynamic Custom Styles for Animations */}
      <style>{`
        @keyframes float-slow {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-10px); }
        }
        @keyframes float-delayed {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-15px); }
        }
        @keyframes blob-bounce {
          0% { transform: translate(0px, 0px) scale(1); }
          33% { transform: translate(30px, -50px) scale(1.1); }
          66% { transform: translate(-20px, 20px) scale(0.9); }
          100% { transform: translate(0px, 0px) scale(1); }
        }
        @keyframes grid-scroll {
          0% { background-position: 0 0; }
          100% { background-position: 40px 40px; }
        }
        .animate-float { animation: float-slow 4s ease-in-out infinite; }
        .animate-float-delayed { animation: float-delayed 5s ease-in-out infinite 1s; }
        .animate-blob-1 { animation: blob-bounce 15s infinite ease-in-out; }
        .animate-blob-2 { animation: blob-bounce 20s infinite ease-in-out reverse; }
        .animate-grid { animation: grid-scroll 3s linear infinite; }
      `}</style>

      {/* Left Panel - Interactive Branding */}
      <div 
        ref={leftPanelRef}
        onMouseMove={handleMouseMove}
        className="hidden lg:flex lg:w-1/2 bg-slate-900 relative overflow-hidden flex-col justify-between p-12 group cursor-default"
      >
        
        {/* Interactive Mouse Glow Effect */}
        <div 
          className="absolute inset-0 z-0 opacity-0 group-hover:opacity-100 transition-opacity duration-700 pointer-events-none"
          style={{
            background: `radial-gradient(600px circle at ${mousePos.x}px ${mousePos.y}px, rgba(59, 130, 246, 0.15), transparent 40%)`
          }}
        />

        {/* Dynamic Animated Background Patterns */}
        <div className="absolute inset-0 z-0 pointer-events-none">
          {/* Moving Orbs */}
          <div className="absolute -top-40 -left-40 w-[600px] h-[600px] bg-blue-600/20 rounded-full mix-blend-screen filter blur-[100px] animate-blob-1"></div>
          <div className="absolute top-1/2 right-0 w-[500px] h-[500px] bg-[#F96D3E]/20 rounded-full mix-blend-screen filter blur-[100px] animate-blob-2"></div>
          <div className="absolute bottom-0 left-1/4 w-[400px] h-[400px] bg-indigo-500/20 rounded-full mix-blend-screen filter blur-[80px] animate-blob-1" style={{ animationDelay: '2s' }}></div>
          
          {/* Scrolling Grid Pattern */}
          <div className="absolute inset-0 opacity-20 animate-grid bg-[linear-gradient(to_right,#ffffff12_1px,transparent_1px),linear-gradient(to_bottom,#ffffff12_1px,transparent_1px)] bg-[size:40px_40px]"></div>
        </div>

        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-16 hover:scale-105 transition-transform duration-300 w-max cursor-pointer">
            <div className="bg-[#F96D3E] p-2.5 rounded-xl shadow-[0_0_20px_rgba(249,109,62,0.4)]">
              <Activity className="h-6 w-6 text-white" />
            </div>
            <span className="text-2xl font-bold text-white tracking-tight">EmpMonitor</span>
          </div>

          <h1 className="text-5xl font-bold text-white leading-tight mb-6">
            Enterprise <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-[#F96D3E] to-blue-400 bg-[length:200%_auto] animate-[gradient_4s_linear_infinite]">
              Workforce Management
            </span>
          </h1>
          <p className="text-slate-400 text-lg max-w-md leading-relaxed">
            Secure, scalable, and intelligent monitoring for modern remote and hybrid teams.
          </p>
        </div>

        <div className="relative z-10 grid grid-cols-2 gap-6 max-w-lg mt-12">
          {/* Card 1 */}
          <div className="animate-float bg-slate-800/40 backdrop-blur-md p-6 rounded-2xl border border-slate-700/50 hover:bg-slate-800/60 hover:border-blue-500/50 transition-all duration-300 group/card cursor-pointer">
            <div className="bg-blue-500/10 w-12 h-12 rounded-xl flex items-center justify-center mb-4 group-hover/card:scale-110 transition-transform">
              <Users className="h-6 w-6 text-blue-400" />
            </div>
            <h3 className="text-white font-semibold mb-1 group-hover/card:text-blue-400 transition-colors">Team Analytics</h3>
            <p className="text-sm text-slate-400">Deep insights into productivity trends.</p>
          </div>
          
          {/* Card 2 */}
          <div className="animate-float-delayed bg-slate-800/40 backdrop-blur-md p-6 rounded-2xl border border-slate-700/50 hover:bg-slate-800/60 hover:border-[#F96D3E]/50 transition-all duration-300 group/card cursor-pointer mt-8">
            <div className="bg-[#F96D3E]/10 w-12 h-12 rounded-xl flex items-center justify-center mb-4 group-hover/card:scale-110 transition-transform">
              <Clock className="h-6 w-6 text-[#F96D3E]" />
            </div>
            <h3 className="text-white font-semibold mb-1 group-hover/card:text-[#F96D3E] transition-colors">Time Tracking</h3>
            <p className="text-sm text-slate-400">Automated precision logging.</p>
          </div>
        </div>
      </div>

      {/* Right Panel - Login Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12 lg:p-24 bg-slate-50 relative">
        <div className="w-full max-w-md">
          
          {/* Mobile Header */}
          <div className="flex lg:hidden items-center justify-center gap-3 mb-10">
            <div className="bg-[#F96D3E] p-2.5 rounded-xl shadow-lg">
              <Activity className="h-6 w-6 text-white" />
            </div>
            <span className="text-2xl font-bold text-slate-900 tracking-tight">EmpMonitor</span>
          </div>

          <div className="bg-white p-8 sm:p-10 rounded-3xl shadow-xl shadow-slate-200/40 border border-slate-100">
            
            {/* Step 1: Credentials */}
            {step === 1 && (
              <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                <div className="mb-8">
                  <h2 className="text-2xl font-bold text-slate-900 mb-2">Admin Login</h2>
                  <p className="text-slate-500 text-sm">Enter your credentials to access the portal.</p>
                </div>
                
                {error && (
                  <div className="mb-6 p-3 bg-red-50 border border-red-100 text-red-600 text-sm rounded-xl font-medium">
                    {error}
                  </div>
                )}

                <form onSubmit={handleCredentialsSubmit} className="space-y-5">
                  <div>
                    <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">Corporate Email</label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-slate-400">
                        <Mail className="h-5 w-5" />
                      </div>
                      <input 
                        type="email" 
                        required
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full pl-12 pr-4 py-3.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all text-sm font-medium text-slate-800"
                        placeholder="admin@empmonitor.com"
                      />
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between items-center mb-2">
                      <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider">Password</label>
                      <a href="#" className="text-xs font-bold text-blue-600 hover:text-blue-700 transition">Forgot password?</a>
                    </div>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-slate-400">
                        <Lock className="h-5 w-5" />
                      </div>
                      <input 
                        type="password" 
                        required
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="w-full pl-12 pr-4 py-3.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all text-sm font-medium text-slate-800"
                        placeholder="••••••••"
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-between mt-2 mb-2">
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input 
                        type="checkbox" 
                        checked={rememberMe}
                        onChange={(e) => setRememberMe(e.target.checked)}
                        className="w-4 h-4 rounded text-blue-600 focus:ring-blue-500 border-slate-300"
                      />
                      <span className="text-sm text-slate-600 font-medium">Remember me</span>
                    </label>
                  </div>

                  <button 
                    type="submit" 
                    className="w-full flex items-center justify-center gap-2 bg-slate-900 text-white py-3.5 rounded-xl text-sm font-bold hover:bg-slate-800 transition shadow-lg shadow-slate-900/20 mt-4"
                  >
                    Authenticate <ArrowRight className="h-4 w-4" />
                  </button>
                </form>
              </div>
            )}

            {/* Step 2: MFA Verification */}
            {step === 2 && (
              <div className="animate-in fade-in slide-in-from-right-4 duration-500">
                <button 
                  onClick={() => { setStep(1); setError(''); }} 
                  className="flex items-center gap-1 text-xs font-bold text-slate-500 hover:text-slate-800 mb-6 transition"
                >
                  <ChevronLeft className="h-4 w-4" /> Back to login
                </button>

                <div className="mb-8">
                  <div className="flex items-center gap-3 mb-2">
                    <ShieldCheck className="h-6 w-6 text-blue-600" />
                    <h2 className="text-2xl font-bold text-slate-900">2-Step Verification</h2>
                  </div>
                  <p className="text-slate-500 text-sm">
                    {isFirstTimeSetup 
                      ? "Scan the QR code with your authenticator app to secure this device."
                      : "Enter the 6-digit verification code generated by your authenticator app."}
                  </p>
                </div>

                {error && (
                  <div className="mb-6 p-3 bg-red-50 border border-red-100 text-red-600 text-sm rounded-xl font-medium text-center">
                    {error}
                  </div>
                )}
                
                {isFirstTimeSetup && (
                  <div className="flex flex-col items-center justify-center mb-8 bg-slate-50 p-6 rounded-2xl border border-slate-200">
                    <div className="bg-white p-3 rounded-2xl shadow-sm border border-slate-100 mb-4 inline-flex items-center justify-center">
                      {qrUrl ? (
                        <img src={qrUrl} alt="Scan to setup MFA" className="w-40 h-40 object-contain" />
                      ) : (
                        <div className="w-40 h-40 flex items-center justify-center bg-slate-50 rounded-xl">
                          <QrCode className="h-10 w-10 text-slate-300" />
                        </div>
                      )}
                    </div>
                    <div className="text-center">
                      <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Manual Setup Key</p>
                      <p className="text-sm font-mono text-slate-800 font-bold bg-white px-4 py-2 rounded-lg border border-slate-200 tracking-widest uppercase">
                        {mfaSecret}
                      </p>
                    </div>
                  </div>
                )}

                <form onSubmit={handleMfaSubmit} className="space-y-6">
                  <div>
                    <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-3 text-center">Verification Code</label>
                    <input 
                      type="text" 
                      required
                      maxLength={6}
                      value={mfaCode}
                      onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, ''))}
                      className="w-full text-center tracking-[0.75em] text-3xl py-4 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all font-mono text-slate-900 shadow-inner"
                      placeholder="000000"
                    />
                  </div>

                  <button 
                    type="submit" 
                    disabled={mfaCode.length !== 6}
                    className="w-full flex items-center justify-center gap-2 bg-blue-600 disabled:bg-blue-300 text-white py-3.5 rounded-xl text-sm font-bold hover:bg-blue-700 transition shadow-lg shadow-blue-500/30"
                  >
                    <CheckCircle2 className="h-5 w-5" /> Verify & Access Dashboard
                  </button>
                </form>
              </div>
            )}

          </div>
          
          <p className="text-center text-slate-400 text-xs mt-8">
            &copy; {new Date().getFullYear()} EmpMonitor Inc. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  );
}
