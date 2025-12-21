import http from './http';

/**
 * 将后端返回的 fileUrl 归一化为可直接被 http(axios) 请求的路径。
 */
const normalizeProofPath = (fileUrl: string) => {
  const normalizePath = (rawPath: string) => {
    let path = rawPath.trim();
    if (path.startsWith('/api/v1/')) {
      path = path.slice('/api/v1'.length);
    }
    if (!path.startsWith('/')) {
      path = `/${path}`;
    }
    return path;
  };

  try {
    const url = new URL(fileUrl, window.location.origin);
    return normalizePath(`${url.pathname}${url.search}`);
  } catch {
    return normalizePath(fileUrl);
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
  // 尽力清理：避免 ObjectURL 长时间占用内存
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 5 * 60 * 1000);
};
