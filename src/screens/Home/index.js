import React, { useState, useEffect } from 'react';
import { View, StyleSheet } from 'react-native';
import { TextInput, Button, SegmentedButtons } from 'react-native-paper';
import Slider from '@react-native-community/slider';
import { loadFonts, getAvailableFonts } from '../../utils/fontLoader';
import { createCanvas, drawText, saveImage } from '../../utils/canvas';

const backgrounds = [
  { name: 'leaf', source: require('../../assets/background/leaf.png') },
  { name: 'moon1', source: require('../../assets/background/moon1.png') },
  { name: 'moon2', source: require('../../assets/background/moon2.png') },
];

const Home = () => {
  const [text, setText] = useState('今天不走，\n    明天要跑。');
  const [previewUrl, setPreviewUrl] = useState(null);
  const [isFontLoaded, setIsFontLoaded] = useState(false);
  const [selectedFont, setSelectedFont] = useState('平方上上谦体');
  const [availableFonts, setAvailableFonts] = useState([]);
  const [selectedBackground, setSelectedBackground] = useState(backgrounds[0]);
  const [fontSizeScale, setFontSizeScale] = useState(0.7); // 字体大小缩放比例

  useEffect(() => {
    // 加载字体
    const initFonts = async () => {
      await loadFonts();
      setIsFontLoaded(true);
      setAvailableFonts(getAvailableFonts());
    };
    initFonts();
  }, []);

  // 计算自适应字体大小
  const calculateFontSize = (canvasSize, text, maxWidth) => {
    const lines = text.split('\n');
    const maxLineLength = Math.max(...lines.map(line => line.length));
    const baseFontSize = Math.floor(canvasSize / 4);
    
    // 根据文本长度调整基础字体大小
    let adjustedFontSize = baseFontSize;
    if (maxLineLength > 10) {
      adjustedFontSize = Math.floor(baseFontSize * (10 / maxLineLength));
    }
    
    // 应用用户设置的缩放比例
    return Math.floor(adjustedFontSize * fontSizeScale);
  };

  const generatePreview = () => {
    if (!text || !isFontLoaded) return;

    const canvasSize = 1000;
    const canvas = createCanvas(canvasSize, canvasSize);
    const ctx = canvas.getContext('2d');
    const img = new window.Image();
    img.src = selectedBackground.source;
    img.onload = () => {
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      
      // 计算自适应字体大小
      const fontSize = calculateFontSize(canvasSize, text, canvasSize * 0.1);
      ctx.font = `${fontSize}px "${selectedFont}"`;
      ctx.fillStyle = '#FFFFFF';
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = fontSize / 60;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      
      const lines = text.split('\n');
      const lineHeight = fontSize * 1.2;
      const totalHeight = lines.length * lineHeight;
      const startY = (canvasSize - totalHeight) / 2;
      
      // 保存当前状态
      ctx.save();
      // 移动到画布中心
      ctx.translate(canvasSize / 2, canvasSize / 2);
      // 旋转 -2 度（负值表示逆时针旋转）
      ctx.rotate(-2 * Math.PI / 180);
      // 移回原位
      ctx.translate(-canvasSize / 2, -canvasSize / 2);
      
      lines.forEach((line, index) => {
        const y = startY + index * lineHeight;
        ctx.strokeText(line, canvasSize / 2, y);
        ctx.fillText(line, canvasSize / 2, y);
      });

      // 恢复之前的状态
      ctx.restore();

      canvas.toBlob((blob) => {
        setPreviewUrl(URL.createObjectURL(blob));
      }, 'image/png');
    };
  };

  const handleSave = async () => {
    if (!previewUrl) return;
    const canvas = document.createElement('canvas');
    const img = new window.Image();
    img.src = previewUrl;
    await new Promise((resolve) => {
      img.onload = resolve;
    });
    canvas.width = img.width;
    canvas.height = img.height;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(img, 0, 0);
    await saveImage(canvas, 'handwriting.png');
  };

  return (
    <View style={styles.container}>
      <TextInput
        mode="outlined"
        label="输入文本"
        value={text}
        onChangeText={setText}
        multiline
        numberOfLines={4}
        style={styles.input}
      />
      
      <View style={styles.fontSelector}>
        <SegmentedButtons
          value={selectedFont}
          onValueChange={setSelectedFont}
          buttons={availableFonts.map(font => ({
            value: font,
            label: font,
          }))}
        />
      </View>

      <View style={styles.backgroundSelector}>
        <SegmentedButtons
          value={selectedBackground.name}
          onValueChange={(value) => setSelectedBackground(backgrounds.find(bg => bg.name === value))}
          buttons={backgrounds.map(bg => ({
            value: bg.name,
            label: bg.name,
          }))}
        />
      </View>

      <View style={styles.fontSizeContainer}>
        <Slider
          value={fontSizeScale}
          onValueChange={setFontSizeScale}
          minimumValue={0.2}
          maximumValue={1.2}
          step={0.1}
          style={styles.slider}
          minimumTrackTintColor="#2196F3"
          maximumTrackTintColor="#000000"
        />
        <Button
          mode="outlined"
          onPress={() => setFontSizeScale(1)}
          style={styles.resetButton}
        >
          重置大小
        </Button>
      </View>

      <View style={styles.buttonContainer}>
        <Button
          mode="contained"
          onPress={generatePreview}
          style={styles.button}
        >
          生成预览
        </Button>
        <Button
          mode="contained"
          onPress={handleSave}
          style={styles.button}
          disabled={!previewUrl}
        >
          保存图片
        </Button>
      </View>

      <View style={styles.previewContainer}>
        {previewUrl && (
          <img src={previewUrl} alt="预览" style={styles.preview} />
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#fff',
  },
  input: {
    marginBottom: 20,
  },
  fontSelector: {
    marginBottom: 20,
  },
  backgroundSelector: {
    marginBottom: 20,
  },
  fontSizeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
  },
  slider: {
    flex: 1,
    marginRight: 10,
  },
  resetButton: {
    marginLeft: 10,
  },
  previewContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    marginBottom: 20,
    minHeight: 300,
  },
  preview: {
    maxWidth: '100%',
    maxHeight: '100%',
    objectFit: 'contain',
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 20,
  },
  button: {
    minWidth: 120,
  },
});

export default Home; 