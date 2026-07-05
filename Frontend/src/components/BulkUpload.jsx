import React, { useState } from 'react';
import { ApiService } from '../services/ApiService';

const BulkUpload = () => {
  const [urls, setUrls] = useState('');
  const [loading, setLoading] = useState(false);
  const [resultsPreview, setResultsPreview] = useState('');

  const handleBulkShorten = async () => {
    setLoading(true);
    setResultsPreview('');
    const urlArray = urls.split('\n').filter(url => url.trim() !== '');
    
    if (urlArray.length === 0) {
      alert('Please enter at least one URL.');
      setLoading(false);
      return;
    }

    let fileContent = "ZIPLINK BULK EXPORT\n-------------------\n\n";
    const BASE_URL = window.location.origin;

    try {
      for (const url of urlArray) {
        const data = await ApiService.shortenUrl({ longUrl: url.trim() });
        fileContent += `Original: ${url}\nShort: ${BASE_URL}/${data.shortCode}\n\n`;
      }

      setResultsPreview(fileContent);

      // Simple file download without body manipulation
      const blob = new Blob([fileContent], { type: 'text/plain' });
      const element = document.createElement('a');
      element.href = URL.createObjectURL(blob);
      element.download = 'shortened_links.txt';
      element.click();
      URL.revokeObjectURL(element.href);
    } catch (err) {
      alert('Bulk processing failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card">
      <h3 className="card-title">Bulk URL Shortener (Batch Processing)</h3>
      <textarea 
        placeholder="Paste multiple URLs (one per line)..." 
        value={urls}
        onChange={(e) => setUrls(e.target.value)}
        className="textarea-field"
      />
      <button 
        onClick={handleBulkShorten} 
        disabled={loading}
        className="btn btn-success btn-block"
        style={{ marginTop: '12px' }}
      >
        {loading ? 'Processing Batch...' : 'Shorten All & Download File'}
      </button>

      {resultsPreview && (
        <div className="bulk-results">
          <div className="bulk-results-title">Processed Links Preview:</div>
          <textarea 
            readOnly 
            value={resultsPreview}
            className="textarea-field"
            style={{ height: '150px' }}
          />
        </div>
      )}
    </div>
  );
};

export default BulkUpload;