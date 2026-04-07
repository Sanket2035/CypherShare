import React, { useState, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Upload, Check, AlertCircle, Loader2 } from 'lucide-react';

const SendPanel = ({ apiUrl }) => {
  const [file, setFile] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [status, setStatus] = useState('idle'); // idle, uploading, success, error
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback(() => {
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      setFile(droppedFile);
      setStatus('idle');
      setError('');
    }
  }, []);

  const handleFileSelect = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      setFile(selectedFile);
      setStatus('idle');
      setError('');
    }
  };

  const handleSend = async () => {
    if (!file) return;

    setStatus('uploading');
    setError('');

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`${apiUrl}/api/relay/send`, {
        method: 'POST',
        headers: {
          'X-Business-License': 'CYPHERSHARE_BUSINESS_2026'
        },
        body: formData
      });

      if (!response.ok) {
        throw new Error('Upload failed');
      }

      const data = await response.json();
      setCode(data.code);
      setStatus('success');
    } catch (err) {
      setError(err.message || 'Upload failed. Please try again.');
      setStatus('error');
    }
  };

  const handleReset = () => {
    setFile(null);
    setCode('');
    setStatus('idle');
    setError('');
  };

  return (
    <div className="h-full flex flex-col">
      <div className="flex items-center gap-3 mb-8">
        <Upload className="w-6 h-6 text-cyan-400" />
        <h2 className="text-xl font-mono tracking-wider text-cyan-400 uppercase">Send File</h2>
      </div>

      <AnimatePresence mode="wait">
        {status === 'success' ? (
          <motion.div
            key="success"
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.9 }}
            className="flex-1 flex flex-col items-center justify-center"
          >
            <div className="text-center">
              <Check className="w-16 h-16 text-emerald-400 mx-auto mb-6" />
              <p className="text-xs font-mono text-slate-400 uppercase tracking-widest mb-4">
                [FILE READY FOR TRANSFER]
              </p>
              <div className="mb-8">
                <p className="text-sm text-slate-400 mb-2">Share this code:</p>
                <div 
                  className="text-6xl md:text-8xl font-mono font-bold text-cyan-400 text-glow-cyan tracking-widest"
                  data-testid="transfer-code-display"
                >
                  {code}
                </div>
              </div>
              <div className="space-y-2 text-xs text-slate-500 font-mono">
                <p>[WAITING FOR RECEIVER...]</p>
                <p className="flex items-center justify-center gap-2">
                  <span className="animate-blink">_</span>
                  <span>CODE EXPIRES IN 10 MINUTES</span>
                </p>
              </div>
              <button
                onClick={handleReset}
                className="mt-8 px-6 py-3 border border-slate-700 text-slate-300 font-mono text-sm tracking-widest uppercase hover:border-slate-500 hover:text-white transition-colors"
                data-testid="send-another-file-btn"
              >
                SEND ANOTHER FILE
              </button>
            </div>
          </motion.div>
        ) : (
          <motion.div
            key="upload"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex-1 flex flex-col"
          >
            {/* Dropzone */}
            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`flex-1 flex flex-col items-center justify-center border-2 border-dashed cursor-pointer transition-all ${
                isDragging
                  ? 'border-cyan-400 bg-cyan-400/5'
                  : 'border-slate-700 hover:border-cyan-400 hover:bg-cyan-400/5'
              }`}
              data-testid="upload-file-dropzone"
            >
              <input
                ref={fileInputRef}
                type="file"
                onChange={handleFileSelect}
                className="hidden"
                data-testid="file-input-hidden"
              />
              <Upload className={`w-16 h-16 mb-4 transition-colors ${
                isDragging ? 'text-cyan-400' : 'text-slate-600'
              }`} />
              {file ? (
                <div className="text-center">
                  <p className="text-lg font-mono text-cyan-400 mb-2">{file.name}</p>
                  <p className="text-sm text-slate-400">
                    {(file.size / 1024 / 1024).toFixed(2)} MB
                  </p>
                </div>
              ) : (
                <div className="text-center">
                  <p className="text-lg text-slate-400 mb-2">Drop file here or click to browse</p>
                  <p className="text-xs text-slate-600 font-mono tracking-wider">MAX SIZE: 5GB</p>
                </div>
              )}
            </div>

            {/* Send Button */}
            {file && status !== 'uploading' && (
              <motion.button
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                onClick={handleSend}
                disabled={status === 'uploading'}
                className="mt-6 w-full bg-cyan-500 text-slate-950 py-4 font-mono text-sm tracking-widest uppercase hover:bg-cyan-400 hover:shadow-[0_0_15px_rgba(6,182,212,0.5)] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                data-testid="send-file-btn"
              >
                INITIATE TRANSFER
              </motion.button>
            )}

            {/* Loading State */}
            {status === 'uploading' && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="mt-6 flex items-center justify-center gap-3 text-cyan-400 font-mono text-sm"
              >
                <Loader2 className="w-5 h-5 animate-spin" />
                <span className="tracking-widest">[PROCESSING...]</span>
              </motion.div>
            )}

            {/* Error State */}
            {status === 'error' && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="mt-6 flex items-center gap-3 p-4 border border-red-400/30 bg-red-400/10 text-red-400 text-sm font-mono"
                data-testid="upload-error-message"
              >
                <AlertCircle className="w-5 h-5" />
                <span>{error}</span>
              </motion.div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default SendPanel;