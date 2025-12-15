import http from './http';

const normalizeProofPath = (fileUrl: string) => {
  try {
    const url = new URL(fileUrl, window.location.origin);
    let path = url.pathname;

    // We call through axios instance with baseURL = /api/v1.
    // So we must pass paths like /proofs/{fileName} (without the /api/v1 prefix).
    if (path.startsWith('/api/v1/')) {
      path = path.slice('/api/v1'.length);
    }

    // Backward compatibility: older records may store /proofs/{fileName}
    if (path.startsWith('/proofs/')) {
      return path;
    }

    // Canonical API: /proofs/{fileName} is the internal path under baseURL.
    if (path.startsWith('/proofs')) {
      return path;
    }

    // If caller passes /api/v1/proofs/{fileName} it becomes /proofs/{fileName}
    if (path.startsWith('/proofs/')) {
      return path;
    }

    return path;
  } catch {
    // Fallback for odd inputs
    return fileUrl.startsWith('/api/v1/') ? fileUrl.slice('/api/v1'.length) : fileUrl;
  }
};

export const fetchProofBlob = async (fileUrl: string): Promise<Blob> => {
  const path = normalizeProofPath(fileUrl);
  const response = await http.get(path, { responseType: 'blob' });
  return response.data as Blob;
};

export const createObjectUrlFromProof = async (fileUrl: string): Promise<string> => {
  const blob = await fetchProofBlob(fileUrl);
  return URL.createObjectURL(blob);
};

export const openProofInNewTab = async (fileUrl: string) => {
  const objectUrl = await createObjectUrlFromProof(fileUrl);
  window.open(objectUrl, '_blank', 'noopener,noreferrer');
  // Best-effort cleanup.
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 5 * 60 * 1000);
};
