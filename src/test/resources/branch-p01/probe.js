// P01 explicit adapter target: a root-relative request created at runtime.
window.p01AssetRequest = fetch('/images/p01.png').then(response => response.ok);
