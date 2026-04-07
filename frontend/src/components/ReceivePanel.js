import React, { useState, useRef, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Download, Loader2, AlertCircle, CheckCircle } from 'lucide-react';

const ReceivePanel = ({ apiUrl }) => {
  const [code, setCode] = useState(['', '', '', '', '', '']);
  const [status, setStatus] = useState('idle'); // idle, downloading, success, error
  const [error, setError] = useState('');
  const inputRefs = useRef([]);

  const handleCodeChange = (index, value) => {
    if (value.length > 1) value = value[0];
    
    const newCode = [...code];
    newCode[index] = value.toUpperCase();
    setCode(newCode);

    // Auto-focus next input
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !code[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pastedText = e.clipboardData.getData('text').toUpperCase().slice(0, 6);
    const newCode = pastedText.split('').concat(Array(6).fill('')).slice(0, 6);
    setCode(newCode);
    
    const lastFilledIndex = pastedText.length - 1;
    if (lastFilledIndex < 5) {
      inputRefs.current[lastFilledIndex + 1]?.focus();
    }
  };

  const handleReceive = async () => {
    const fullCode = code.join('');
    if (fullCode.length !== 6) {
      setError('Please enter a complete 6-digit code');
      return;
    }

    setStatus('downloading');
    setError('');

    try {
      const response = await fetch(`${apiUrl}/api/relay/receive/${fullCode}`);
      
      if (!response.ok) {
        throw new Error('Invalid or expired code');
      }

      const blob = await response.blob();
      const filename = response.headers.get('X-Original-Filename') || 'file';
      const udefFilename = `${filename}.udef`;

      // Download file
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = udefFilename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      setStatus('success');
      setTimeout(() => {
        setStatus('idle');
        setCode(['', '', '', '', '', '']);
      }, 3000);
    } catch (err) {
      setError(err.message || 'Download failed. Please check the code and try again.');
      setStatus('error');
    }
  };

  useEffect(() => {
    inputRefs.current[0]?.focus();
  }, []);

  return (
    <div className="h-full flex flex-col">
      <div className="flex items-center gap-3 mb-8">
        <Download className="w-6 h-6 text-emerald-400" />
        <h2 className="text-xl font-mono tracking-wider text-emerald-400 uppercase">Receive File</h2>
      </div>

      <div className="flex-1 flex flex-col justify-center">
        <div className="mb-8">
          <p className="text-sm text-slate-400 mb-6 text-center">Enter 6-digit transfer code:</p>
          
          {/* Code Input */}
          <div className="flex gap-2 justify-center mb-6" onPaste={handlePaste}>
            {code.map((digit, index) => (
              <input
                key={index}
                ref={(el) => (inputRefs.current[index] = el)}
                type="text"
                maxLength="1"
                value={digit}
                onChange={(e) => handleCodeChange(index, e.target.value)}
                onKeyDown={(e) => handleKeyDown(index, e)}
                className="w-12 h-16 md:w-16 md:h-20 bg-transparent border-b-2 border-slate-700 text-center text-3xl md:text-4xl font-mono text-cyan-400 focus:outline-none focus:border-cyan-400 transition-colors"
                data-testid={`code-input-field-${index + 1}`}
              />
            ))}
          </div>
        </div>

        {/* Download Button */}
        <button
          onClick={handleReceive}
          disabled={status === 'downloading' || code.join('').length !== 6}
          className="w-full bg-emerald-500 text-slate-950 py-4 font-mono text-sm tracking-widest uppercase hover:bg-emerald-400 hover:shadow-[0_0_15px_rgba(52,211,153,0.5)] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          data-testid="download-udef-button"
        >
          {status === 'downloading' ? (
            <span className="flex items-center justify-center gap-2">
              <Loader2 className="w-5 h-5 animate-spin" />
              DOWNLOADING...
            </span>
          ) : (
            'RECEIVE FILE'
          )}
        </button>

        {/* Status Messages */}
        {status === 'success' && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-6 flex items-center gap-3 p-4 border border-emerald-400/30 bg-emerald-400/10 text-emerald-400 text-sm font-mono justify-center"
            data-testid="download-success-message"
          >
            <CheckCircle className="w-5 h-5" />
            <span>[DOWNLOAD COMPLETE]</span>
          </motion.div>
        )}

        {status === 'error' && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-6 flex items-center gap-3 p-4 border border-red-400/30 bg-red-400/10 text-red-400 text-sm font-mono"
            data-testid="download-error-message"
          >
            <AlertCircle className="w-5 h-5" />
            <span>{error}</span>
          </motion.div>
        )}

        {/* Info Text */}
        <div className="mt-8 text-center">
          <p className="text-xs text-slate-600 font-mono tracking-wider">
            [UDEF ENCRYPTED FILE WILL BE DOWNLOADED]
          </p>
        </div>
      </div>
    </div>
  );
};

export default ReceivePanel;