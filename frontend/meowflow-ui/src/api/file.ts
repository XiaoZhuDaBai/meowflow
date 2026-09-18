import http from './http';
import { serviceUrl } from './endpoints';

export interface UploadedFile {
  id: string;
  name: string;
  contentType?: string;
  size: number;
  path?: string;
  url?: string;
  storage?: string;
}

export const fileApi = {
  upload(file: File, onProgress?: (percent: number) => void): Promise<UploadedFile> {
    const formData = new FormData();
    formData.append('file', file);
    return http
      .post(serviceUrl('workflow', '/api/file/upload'), formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (event) => {
          if (onProgress && event.total) {
            onProgress(Math.round((event.loaded * 100) / event.total));
          }
        },
      })
      .then((r: any) => r.data as UploadedFile);
  },
};
