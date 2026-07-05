import React, { useEffect, useState } from 'react';
import { ApiService } from '../services/ApiService';
import PasswordPrompt from './PasswordPrompt';

const RedirectHandler = ({ code }) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [requiresPassword, setRequiresPassword] = useState(false);

  useEffect(() => {
    const handleRedirect = async () => {
      try {
        const data = await ApiService.getRedirectInfo(code);

        if (data.requiresPassword) {
          setRequiresPassword(true);
          setLoading(false);
          return;
        }

        if (data.redirect && data.url) {
          window.location.href = data.url;
          return;
        }

        // Fallback direct redirect
        const API_URL = import.meta.env.VITE_API_URL || '';
        window.location.href = `${API_URL}/api/redirect/${code}`;
      } catch (err) {
        if (err.response?.status === 404) {
          setError(err.response.data?.message || 'Link not found or has been deleted.');
        } else if (err.response?.status === 410) {
          setError(err.response.data?.message || 'This link has expired.');
        } else {
          setError('Failed to redirect. Please try again.');
        }
        setLoading(false);
      }
    };

    handleRedirect();
  }, [code]);

  if (loading) {
    return (
      <div className="redirect-container">
        <div style={{ textAlign: 'center' }}>
          <div className="icon-big">⏳</div>
          <div>Redirecting...</div>
        </div>
      </div>
    );
  }

  if (requiresPassword) {
    return <PasswordPrompt code={code} />;
  }

  if (error) {
    return (
      <div className="redirect-container">
        <div className="redirect-card">
          <div className="icon-big">❌</div>
          <h2 className="card-title">Error</h2>
          <div className="error-message">{error}</div>
          <button 
            onClick={() => window.location.href = '/'}
            className="btn btn-primary btn-block"
            style={{ marginTop: '20px' }}
          >
            Go Home
          </button>
        </div>
      </div>
    );
  }

  return null;
};

export default RedirectHandler;
