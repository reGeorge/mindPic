const createExpoWebpackConfigAsync = require('@expo/webpack-config');

module.exports = async function (env, argv) {
  const config = await createExpoWebpackConfigAsync(env, argv);
  
  // 添加 MIME 类型处理
  config.module.rules.push({
    test: /\.(png|jpg|gif|ttf|woff|woff2|eot|svg)$/i,
    use: [
      {
        loader: 'file-loader',
        options: {
          name: '[name].[ext]',
          outputPath: 'assets/',
          publicPath: '/assets/',
        },
      },
    ],
  });

  // 添加 Buffer polyfill
  config.resolve.fallback = {
    ...config.resolve.fallback,
    buffer: require.resolve('buffer/'),
    stream: require.resolve('stream-browserify'),
    crypto: require.resolve('crypto-browserify'),
  };

  return config;
}; 