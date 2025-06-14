import { Platform } from 'react-native';
import Canvas from 'react-native-canvas';

// Canvas工具函数

// 在Canvas上绘制文本
export const drawText = async (canvas, text, options = {}) => {
  const {
    font = '16px Arial',
    color = '#000000',
    x = 0,
    y = 0,
    maxWidth = canvas.width,
    lineHeight = 20,
  } = options;

  if (Platform.OS === 'web') {
    const ctx = canvas.getContext('2d');
    ctx.font = font;
    ctx.fillStyle = color;

    const words = text.split(' ');
    let line = '';
    let currentY = y;

    for (let n = 0; n < words.length; n++) {
      const testLine = line + words[n] + ' ';
      const metrics = ctx.measureText(testLine);
      const testWidth = metrics.width;

      if (testWidth > maxWidth && n > 0) {
        ctx.fillText(line, x, currentY);
        line = words[n] + ' ';
        currentY += lineHeight;
      } else {
        line = testLine;
      }
    }
    ctx.fillText(line, x, currentY);
  } else {
    const ctx = canvas.getContext('2d');
    // 移动端：直接设置属性，而不是调用方法
    const fontSizeMatch = font.match(/(\d+)px/);
    const actualFontSize = fontSizeMatch ? parseFloat(fontSizeMatch[1]) : parseFloat(font);

    // 暂时使用通用字体，以测试是否为自定义字体导致的问题
    ctx.font = `${actualFontSize}px Arial`;
    ctx.fillStyle = color;

    const lines = text.split('\n');
    const calculatedLineHeight = actualFontSize * 1.2;
    let currentY = y - (lines.length / 2) * calculatedLineHeight;

    for (const line of lines) {
      ctx.fillText(line, x, currentY);
      currentY += calculatedLineHeight;
    }
  }
};

// 将Canvas转换为图片
export const canvasToImage = async (canvas) => {
  if (Platform.OS === 'web') {
    return new Promise((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (blob) {
          resolve(URL.createObjectURL(blob));
        } else {
          console.error('canvas.toBlob() returned null for web platform.');
          resolve(null);
        }
      }, 'image/png');
    });
  } else {
    // 移动端
    let dataUrl = null;
    try {
      console.log('Before canvas.toDataURL() call. Canvas object type:', typeof canvas, ', Is canvas truthy?', !!canvas);

      await new Promise(resolve => setTimeout(resolve, 50)); // 增加延迟以确保渲染完成
      dataUrl = await canvas.toDataURL();
      console.log('After canvas.toDataURL() call. Result type:', typeof dataUrl);
      console.log('canvas.toDataURL() mobile result (full):', dataUrl);

      if (typeof dataUrl === 'string' && dataUrl.startsWith('data:image')) {
        console.log('canvas.toDataURL() successful, returning data URL.');
        return dataUrl;
      } else {
        console.error('canvas.toDataURL() on mobile returned invalid data URL format:', dataUrl);
        return null;
      }
    } catch (error) {
      console.error('Caught error during canvas.toDataURL() on mobile:', typeof error, error);
      if (error.stack) {
        console.error('Error stack:', error.stack);
      }
      // 确保抛出的是一个标准的、可序列化的 Error 对象
      let serializableError;
      if (error instanceof Error) {
        serializableError = error;
      } else if (typeof error === 'object' && error !== null && error.message) {
        serializableError = new Error(error.message);
        if (error.stack) serializableError.stack = error.stack; // 尝试保留堆栈信息
      } else {
        serializableError = new Error(`非标准错误: ${String(error)}`);
      }
      throw new Error(`Failed to convert canvas to image (mobile): ${serializableError.message}`); 
    }
  }
};

// 保存图片
export const saveImage = async (canvas, filename = 'handwriting.png') => {
  if (Platform.OS === 'web') {
    const link = document.createElement('a');
    link.download = filename;
    link.href = await canvasToImage(canvas);
    link.click();
  } else {
    const base64 = await canvasToImage(canvas);
    const { writeAsStringAsync, documentDirectory } = await import('expo-file-system');
    const fileUri = `${documentDirectory}${filename}`;
    await writeAsStringAsync(fileUri, base64, { encoding: 'base64' });
    const { shareAsync } = await import('expo-sharing');
    await shareAsync(fileUri);
  }
}; 