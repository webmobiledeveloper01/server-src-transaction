import { useState, useEffect, Dispatch, SetStateAction } from 'react';

type TransformFunction<T> = (value: string | null) => T;

export const useSessionStorage = <T>(
  key: string,
  initialValue: T,
  transform: TransformFunction<T>
): [T, Dispatch<SetStateAction<T>>] => {
  const [value, setValue] = useState<T>(() => {
    
    const storageValue = localStorage.getItem(key);
    if(storageValue !='' && storageValue != null) {
      const storedOrderDetail = JSON.parse(localStorage.getItem(key) || '[]');
      return transform({ ...storedOrderDetail });
    } else {
      return initialValue;
    }
  });

  useEffect(() => {
    localStorage.setItem(key, JSON.stringify(value));
  }, [key, value]);

  return [value, setValue];
};

//export default useSessionStorage;
