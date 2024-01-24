import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

const LINKING_ERROR = '请检查是否正确引用AndroidUpdater模块';

const AndroidUpdaterModule = NativeModules.AndroidUpdater
  ? NativeModules.AndroidUpdater
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export const AndroidUpdaterEmitter = new NativeEventEmitter(
  AndroidUpdaterModule
);

export type AndroidUpdaterProps = {
  url: string;
  md5?: string;
};

export type AndroidUpdaterResultProps = {
  downUrl: string;
  description: string;
  versionName: string;
  versionCode: string;
  updateType: number;
  size?: number | string;
};

/**
 * 下载并安装apk
 * @param props
 */
export const downloadApk = (props: AndroidUpdaterProps): Promise<boolean> => {
  if (Platform.OS === 'ios') {
    return Promise.resolve(true);
  }
  return AndroidUpdaterModule.downloadApk(props);
};

/**
 * 取消下载
 */
export const cancelDownloadApk = (): Promise<boolean> => {
  if (Platform.OS === 'ios') {
    return Promise.resolve(true);
  }
  return AndroidUpdaterModule.cancelDownloadApk();
};

/**
 * 安装
 */
export const installApk = (): Promise<boolean> => {
  if (Platform.OS === 'ios') {
    return Promise.resolve(true);
  }
  return AndroidUpdaterModule.installApk();
};
