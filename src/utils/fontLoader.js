import { Platform } from 'react-native';

// 字体文件映射
const fontMap = {
  '平方韶华体': require('../assets/fonts/平方韶华体.ttf'),
  '平方上上谦体': require('../assets/fonts/平方上上谦体.ttf'),
  '平方洒脱体': require('../assets/fonts/平方洒脱体.ttf'),
  // 在这里添加字体文件映射
  // 例如：'Handwriting': require('../assets/fonts/Handwriting.ttf'),
};

// 加载字体文件
export const loadFonts = async () => {
  if (Platform.OS === 'web') {
    // Web端字体加载
    const fontFaceSet = document.fonts;
    for (const [fontName, fontPath] of Object.entries(fontMap)) {
      const font = new FontFace(fontName, `url(${fontPath})`);
      await font.load();
      fontFaceSet.add(font);
    }
  } else {
    // 移动端字体加载
    // 使用 react-native-font-loader 加载字体
    // 具体实现待补充
  }
};

// 获取可用字体列表
export const getAvailableFonts = () => {
  return Object.keys(fontMap);
};

// 检查字体是否已加载
export const isFontLoaded = (fontName) => {
  if (Platform.OS === 'web') {
    return document.fonts.check(`12px "${fontName}"`);
  }
  // 移动端实现待补充
  return false;
}; 