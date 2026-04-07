import React, { useState, useEffect } from 'react';
import './App.css';
import { motion } from 'framer-motion';
import { Upload, Download, Shield, Activity } from 'lucide-react';
import SendPanel from './components/SendPanel';
import ReceivePanel from './components/ReceivePanel';

const API_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';

function App() {
  const [activeSessions, setActiveSessions] = useState(0);

  useEffect(() => {
    // Fetch active sessions count
    const fetchHealth = async () => {
      try {
        const response = await fetch(`${API_URL}/api/relay/health`);
        const data = await response.json();
        setActiveSessions(data.activeSessions || 0);
      } catch (error) {
        console.error('Health check failed:', error);
      }
    };

    fetchHealth();
    const interval = setInterval(fetchHealth, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="App min-h-screen bg-slate-950 relative overflow-hidden">
      {/* Ambient Background */}
      <div 
        className="fixed inset-0 opacity-10 mix-blend-screen pointer-events-none"
        style={{
          backgroundImage: 'url(https://static.prod-images.emergentagent.com/jobs/9d0b437b-b74d-4690-b09d-ca5c3f596bb4/images/49a7bbbfbdfb7d7949ce9c2aed3a549061e5c591cbb388e6946c84b9f324f3e1.png)',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          backgroundAttachment: 'fixed'
        }}
      />

      {/* Navigation */}
      <motion.nav 
        initial={{ y: -100 }}
        animate={{ y: 0 }}
        className="sticky top-0 z-50 bg-slate-950/80 backdrop-blur-xl border-b border-slate-800"
        data-testid="main-navigation"
      >
        <div className="container mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Shield className="w-8 h-8 text-cyan-400" />
            <h1 className="text-2xl font-mono font-semibold tracking-wider text-cyan-400">
              CYPHERSHARE
            </h1>
          </div>
          <div className="flex items-center gap-3 font-mono text-xs tracking-widest uppercase">
            <Activity className="w-4 h-4 text-yellow-400 animate-pulse" />
            <span className="text-slate-400">ACTIVE SESSIONS:</span>
            <span className="text-yellow-400 font-bold" data-testid="active-sessions-count">{activeSessions}</span>
          </div>
        </div>
      </motion.nav>

      {/* Main Grid */}
      <main className="container mx-auto p-6 md:p-12">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="grid grid-cols-1 md:grid-cols-12 gap-px bg-slate-800 border border-slate-800 shadow-2xl"
        >
          {/* Send Panel */}
          <div className="col-span-12 md:col-span-7 bg-slate-950 p-6 md:p-12">
            <SendPanel apiUrl={API_URL} />
          </div>

          {/* Receive Panel */}
          <div className="col-span-12 md:col-span-5 bg-slate-950 p-6 md:p-12 border-t md:border-t-0 md:border-l border-slate-800">
            <ReceivePanel apiUrl={API_URL} />
          </div>
        </motion.div>

        {/* Info Section */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="mt-12 border border-slate-800 bg-slate-950 p-8 text-center"
        >
          <div className="flex items-center justify-center gap-4 mb-4">
            <img 
              src="https://static.prod-images.emergentagent.com/jobs/9d0b437b-b74d-4690-b09d-ca5c3f596bb4/images/75978849ac40ae103c55cc7c36ab3bfb8007d6c1eb4dd982c9497270ecee70d4.png"
              alt="Security"
              className="w-16 h-16 opacity-80"
            />
          </div>
          <h3 className="text-xl font-mono text-cyan-400 mb-2 tracking-wider">UDEF PROTOCOL</h3>
          <p className="text-sm text-slate-400 max-w-2xl mx-auto">
            Dictionary-based lossless compression + AES-256-GCM encryption. Zero-storage relay architecture.
            Files are encrypted and compressed in real-time during transfer.
          </p>
        </motion.div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800 mt-12 py-6 text-center text-xs text-slate-500 font-mono tracking-widest">
        <p>[SYSTEM STATUS: OPERATIONAL] • SESSION TIMEOUT: 10 MIN</p>
      </footer>
    </div>
  );
}

export default App;