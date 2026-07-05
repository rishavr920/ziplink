import React, { useState } from 'react';
import { ApiService } from '../services/ApiService';

const PasswordPrompt = ({ code }) => {
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const data = await ApiService.verifyPassword(code, password);
      
      if (data.success) {
        window.location.href = data.originalUrl;
      } else {
        setError('Failed to verify password. Please try again.');
      }
    } catch (err) {
      if (err.response?.status === 401) {
        setError('Incorrect password. Please try again.');
      } else if (err.response?.status === 404) {
        setError('Link not found or has been deleted.');
      } else if (err.response?.status === 410) {
        setError('This link has expired.');
      } else {
        setError('Failed to verify password. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="redirect-container">
      <div className="redirect-card">
        <div className="icon-big">🔒</div>
        <h2 className="card-title">Password Protected Link</h2>
        <p style={{ color: '#94a3b8', marginBottom: '24px' }}>
          This link requires a password to access.
        </p>
        
        {error && <div className="error-message">{error}</div>}
        
        <form onSubmit={handleSubmit} className="form">
          <input 
            type="password" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter password" 
            required
            autoFocus
            className="input-field"
          />
          <button 
            type="submit" 
            disabled={loading}
            className="btn btn-primary btn-block"
          >
            {loading ? 'Verifying...' : 'Access Link'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default PasswordPrompt;
