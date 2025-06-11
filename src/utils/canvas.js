// Canvas工具函数

// 创建Canvas元素
export const createCanvas = (width, height) => {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  return canvas;
};

// 在Canvas上绘制文本
export const drawText = (canvas, text, options = {}) => {
  const {
    font = '16px Arial',
    color = '#000000',
    x = 0,
    y = 0,
    maxWidth = canvas.width,
    lineHeight = 20,
  } = options;

  const ctx = canvas.getContext('2d');
  ctx.font = font;
  ctx.fillStyle = color;

  // 文本换行处理
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
};

// 将Canvas转换为图片
export const canvasToImage = (canvas) => {
  return new Promise((resolve) => {
    canvas.toBlob((blob) => {
      resolve(URL.createObjectURL(blob));
    }, 'image/png');
  });
};

// 保存图片
export const saveImage = async (canvas, filename = 'handwriting.png') => {
  const link = document.createElement('a');
  link.download = filename;
  link.href = await canvasToImage(canvas);
  link.click();
}; 