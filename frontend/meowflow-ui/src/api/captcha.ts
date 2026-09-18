/**
 * 图形验证码 API（与后端 CaptchaController 对齐）
 */
import http from './http';
import { serviceUrl } from './endpoints';

export interface CaptchaVO {
  uuid: string;
  img: string; // base64 data URL
}

export const captchaApi = {
  /**
   * 获取图形验证码图片
   * @returns CaptchaVO，包含 uuid（后续验证时传回）和 img（base64 data URL）
   */
  generate(): Promise<CaptchaVO> {
    return http
      .get(serviceUrl('user', '/api/v1/captcha/generate', '/captcha/generate'))
      .then((r: any) => r.data as CaptchaVO);
  },
};
