import React from 'react';
import { Provider as PaperProvider } from 'react-native-paper';
import { SafeAreaView, StyleSheet } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import Home from './screens/Home';

const App = () => {
  return (
    <PaperProvider
      settings={{
        icon: props => <MaterialCommunityIcons {...props} />,
      }}
    >
      <SafeAreaView style={styles.container}>
        <Home />
      </SafeAreaView>
    </PaperProvider>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
});

export default App; 