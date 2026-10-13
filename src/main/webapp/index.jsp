<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>JSR-352 Ratings Chunk Batcher</title>
</head>
<body>
<h1>JSR-352 Ratings Chunk Batcher</h1>
<p>Jakarta Batch application that reads ratings JSON from a URL, enriches statement data via HTTP calls, and writes finalized JSON output.</p>
<ul>
    <li><a href="batch">Batch Servlet Endpoint (/batch)</a></li>
    <li>REST API Endpoint: <code>POST /api/batch/start?inputUrl={URL}&outputPath={PATH}&apiBaseUrl={API}</code></li>
    <li>Status Endpoint: <code>GET /api/batch/status/{executionId}</code></li>
</ul>
</body>
</html>