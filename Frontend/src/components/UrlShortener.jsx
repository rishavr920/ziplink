import React, { useState } from 'react';
import { ApiService } from '../services/ApiService';

const UrlShortener = () => {
  const [longUrl, setLongUrl] = useState('');
  const [password, setPassword] = useState('');
  const [expiryHours, setExpiryHours] = useState('');
  const [isOneTime, setIsOneTime] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [copySuccess, setCopySuccess] = useState(false);

  const BASE_URL = import.meta.env.VITE_BASE_URL || window.location.origin;

  const handleCopy = async () => {
    const link = `${BASE_URL}/${result.shortCode}`;
    try {
      await navigator.clipboard.writeText(link);
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    } catch (err) {
      alert("Failed to copy link. Please copy it manually.");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    
    try {
      const payload = { 
        longUrl,
        ...(password && { password }),
        ...(expiryHours && { expiryHours: parseInt(expiryHours) }),
        ...(isOneTime && { isOneTime: true })
      };
      
      const data = await ApiService.shortenUrl(payload);
      setResult(data);
      
      // Reset form
      setLongUrl('');
      setPassword('');
      setExpiryHours('');
      setIsOneTime(false);
    } catch (err) {
      if (err.response?.data?.details) {
        setError(err.response.data.details.map(d => d.message).join(', '));
      } else {
        setError(err.response?.data?.error || 'Failed to shorten URL. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card">
      <h3 className="card-title">Single URL Shortener</h3>
      
      {error && <div className="error-message">{error}</div>}
      
      <form onSubmit={handleSubmit} className="form">
        <input 
          type="url" 
          value={longUrl} 
          onChange={(e) => setLongUrl(e.target.value)}
          placeholder="Paste URL (http:// or https://)..." 
          required
          className="input-field"
        />
        
        <div className="input-row">
          <input 
            type="password" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Password (optional)" 
            className="input-field"
          />
          <input 
            type="number" 
            value={expiryHours} 
            onChange={(e) => setExpiryHours(e.target.value)}
            placeholder="Expiry (hours)" 
            min="1"
            max="8760"
            className="input-field"
          />
        </div>
        
        <label className="checkbox-container">
          <input 
            type="checkbox" 
            checked={isOneTime} 
            onChange={(e) => setIsOneTime(e.target.checked)}
          />
          <span>One-time link (deletes after first use)</span>
        </label>
        
        <button 
          type="submit" 
          disabled={loading}
          className="btn btn-primary btn-block"
        >
          {loading ? 'Shortening...' : 'Shorten URL'}
        </button>
      </form>

      {result && (
        <div className="result-container">
          <a 
            href={`${BASE_URL}/${result.shortCode}`}
            target="_blank"
            rel="noopener noreferrer"
            className="result-link"
          >
            {BASE_URL}/{result.shortCode}
          </a>
          
          <div>
            <button 
              onClick={handleCopy} 
              className={`btn btn-copy ${copySuccess ? 'copied' : ''}`}
            >
              {copySuccess ? '✓ Copied!' : 'Copy Link'}
            </button>
          </div>
          
          <img src={result.qrCode} className="qr-code-img" alt="QR Code" />
          
          <div className="info-badges">
            {result.isPasswordProtected && <span className="badge-warning">🔒 Password Protected</span>}
            {result.isOneTime && <span className="badge-danger">⚠️ One-Time Link</span>}
            {result.expiresAt && (
              <div className="expiry-text">
                Expires: {new Date(result.expiresAt).toLocaleString()}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default UrlShortener;