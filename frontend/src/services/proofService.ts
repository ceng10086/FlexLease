import http from './http';

/**
 * 将后端返回的 fileUrl 归一化为可直接被 http(axios) 请求的路径。
 */
const normalizeProofPath = (fileUrl: string) => {
  try {
    const url = new URL(fileUrl, window.location.origin);
    let path = url.pathname;

    // http(axios) 的 baseURL = /api/v1，因此这里统一把输入归一化为 `/proofs/{fileName}`（不带 /api/v1 前缀）。
    if (path.startsWith('/api/v1/')) {
      path = path.slice('/api/v1'.length);
    }

    // 兼容旧数据：历史记录可能存的是 `/proofs/{fileName}`
    if (path.startsWith('/proofs/')) {
      return path;
    }

    // 规范形式：`/proofs/{fileName}`
    if (path.startsWith('/proofs')) {
      return path;
    }

    // 如果调用方传的是 `/api/v1/proofs/{fileName}`，前面已去掉 `/api/v1`，最终也会变成 `/proofs/{fileName}`
    if (path.startsWith('/proofs/')) {
      return path;
    }

    return path;
  } catch {
    // 兜底：处理不规范输入
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
  // 尽力清理：避免 ObjectURL 长时间占用内存
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 5 * 60 * 1000);
};
