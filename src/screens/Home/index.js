import React, { useState, useEffect } from 'react';
import { View, StyleSheet, Platform, Image } from 'react-native';
import { TextInput, Button, SegmentedButtons } from 'react-native-paper';
import Slider from '@react-native-community/slider';
import { loadFonts, getAvailableFonts } from '../../utils/fontLoader';
import { drawText, saveImage, canvasToImage } from '../../utils/canvas';
import * as FileSystem from 'expo-file-system';
import Canvas from 'react-native-canvas';
import { Asset } from 'expo-asset';

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
  const [fontSizeScale, setFontSizeScale] = useState(0.7);
  const [isLoading, setIsLoading] = useState(false);
  const [canvasInstance, setCanvasInstance] = useState(null);

  useEffect(() => {
    const initFonts = async () => {
      try {
        await loadFonts();
        setIsFontLoaded(true);
        setAvailableFonts(getAvailableFonts());
      } catch (error) {
        console.error('字体加载失败:', error);
      }
    };
    initFonts();
  }, []);

  const handleCanvasLoad = async (canvas) => {
    if (Platform.OS !== 'web' && canvas) {
      const canvasSize = 1000;
      await new Promise(resolve => {
        canvas.width = canvasSize;
        canvas.height = canvasSize;
        resolve();
      });
      console.log('Canvas dimensions set via onLoad prop.');
      setCanvasInstance(canvas);
    } else if (Platform.OS === 'web' && canvas) {
      const canvasSize = 1000;
      canvas.width = canvasSize;
      canvas.height = canvasSize;
      setCanvasInstance(canvas);
    }
  };

  const calculateFontSize = (canvasSize, text, maxWidth) => {
    const lines = text.split('\n');
    const maxLineLength = Math.max(...lines.map(line => line.length));
    const baseFontSize = Math.floor(canvasSize / 4);
    
    let adjustedFontSize = baseFontSize;
    if (maxLineLength > 10) {
      adjustedFontSize = Math.floor(baseFontSize * (10 / maxLineLength));
    }
    
    return Math.floor(adjustedFontSize * fontSizeScale);
  };

  const generatePreview = async () => {
    if (!text || !isFontLoaded || !canvasInstance) return;
    
    try {
      setIsLoading(true);
      const canvas = canvasInstance;
      const canvasSize = 1000;
      
      const ctx = canvas.getContext('2d');

      if (Platform.OS === 'web') {
        const img = new window.Image();
        img.src = selectedBackground.source;
        await new Promise((resolve) => {
          img.onload = resolve;
        });
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      } else {
        const asset = Asset.fromModule(selectedBackground.source);
        await asset.downloadAsync();
        
        const localUri = asset.localUri || asset.uri;
        const base64Image = await FileSystem.readAsStringAsync(localUri, {
          encoding: FileSystem.EncodingType.Base64,
        });
        
        const img = new Canvas.Image(canvas);
        img.src = `data:image/png;base64,${base64Image}`;
        await new Promise(resolve => {
          img.addEventListener('load', () => resolve());
        });
        console.log('Before drawImage in generatePreview (Android):');
        console.log('  typeof img:', typeof img);
        console.log('  img.src (first 50 chars):', img.src ? img.src.substring(0, 50) : 'null');
        console.log('  img instanceof Canvas.Image:', img instanceof Canvas.Image);
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      }
      
      const fontSize = calculateFontSize(canvasSize, text, canvasSize * 0.1);
      await drawText(canvas, text, {
        font: `${fontSize}px "${selectedFont}"`,
        color: '#FFFFFF',
        x: canvasSize / 2,
        y: canvasSize / 2,
        maxWidth: canvasSize * 0.8,
        lineHeight: fontSize * 1.2
      });

      const imageUrl = await canvasToImage(canvas);
      console.log('imageUrl before setPreviewUrl:', typeof imageUrl, imageUrl ? imageUrl.substring(0, 100) : imageUrl);
      if (imageUrl && typeof imageUrl === 'string') {
        // setPreviewUrl(imageUrl); // 暂时注释掉这行，以隔离问题
      } else {
        console.error('canvasToImage did not return a valid image URL:', imageUrl);
        // setPreviewUrl(null); // 暂时注释掉这行，以隔离问题
      }
    } catch (error) {
      console.error('生成预览失败: 捕获到错误.', {
        errorObject: error, // 打印原始错误对象
        name: error ? error.name : 'UnknownError',
        message: error ? error.message : 'No message',
        stack: error ? error.stack : 'No stack',
        // 尝试 stringify 错误对象，排除可能导致循环引用的属性
        stringifiedError: JSON.stringify(error, (key, value) => {
          if (value === window || value === document) { // 避免循环引用或不可序列化的DOM对象
            return undefined;
          }
          // 过滤掉可能引起问题的复杂对象，或者只保留基本类型
          if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
            // 简单地返回其类型以避免深度复制或复杂结构
            // return `[Object ${value.constructor ? value.constructor.name : 'Anonymous'}]`;
            // 尝试更安全的序列化：只保留可枚举属性
            return Object.fromEntries(Object.entries(value).filter(([k, v]) => typeof v !== 'function' && typeof v !== 'object' || v === null));
          }
          return value;
        })
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async () => {
    if (!previewUrl || !canvasInstance) return;
    
    try {
      setIsLoading(true);
      const canvas = canvasInstance;
      const canvasSize = 1000;

      const ctx = canvas.getContext('2d');

      if (Platform.OS === 'web') {
        const img = new window.Image();
        img.src = previewUrl;
        await new Promise((resolve) => {
          img.onload = resolve;
        });
        ctx.drawImage(img, 0, 0);
      } else {
        const img = new Canvas.Image(canvas);
        img.src = previewUrl;
        await new Promise(resolve => {
          img.addEventListener('load', () => resolve());
        });
        console.log('Before drawImage in handleSave (Android):');
        console.log('  typeof img:', typeof img);
        console.log('  img.src (first 50 chars):', img.src ? img.src.substring(0, 50) : 'null');
        console.log('  img instanceof Canvas.Image:', img instanceof Canvas.Image);
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      }

      await saveImage(canvas, 'handwriting.png');
    } catch (error) {
      console.error('保存图片失败: 一个非可序列化错误对象被捕获.', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Canvas 
        ref={(canvas) => {
          if (canvas) {
            handleCanvasLoad(canvas);
          }
        }}
        style={{ width: 0, height: 0, position: 'absolute', opacity: 0 }}
      />

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
          loading={isLoading}
          disabled={isLoading}
        >
          生成预览
        </Button>
        <Button
          mode="contained"
          onPress={handleSave}
          style={styles.button}
          loading={isLoading}
          disabled={!previewUrl || isLoading}
        >
          保存图片
        </Button>
      </View>

      <View style={styles.previewContainer}>
        {previewUrl && (
          Platform.OS === 'web' ? (
            <img src={previewUrl} alt="预览" style={styles.preview} />
          ) : (
            <Image 
              source={{ uri: previewUrl }} 
              style={styles.preview}
              resizeMode="contain"
            />
          )
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
    resizeMode: 'contain',
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