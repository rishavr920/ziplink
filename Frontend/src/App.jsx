import React from 'react';
import UrlShortener from './components/UrlShortener';
import BulkUpload from './components/BulkUpload';

function App() {
  return (
    <div className="app-container">
      <div className="content-wrapper">
        <div className="header">
          <h1 className="title">ZipLink</h1>
          <p className="subtitle">Distributed & Scalable URL Engine</p>
        </div>

        <UrlShortener />
        <div className="divider" />
        <BulkUpload />
      </div>
    </div>
  );
}

export default App;