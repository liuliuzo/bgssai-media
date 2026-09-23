import type { ReactElement } from 'react';
export interface PhoneDialFieldProps {
  id?: string;
  dialCode?: string;
  nationalNumber: string;
  onDialCodeChange: (value: string) => void;
  onNationalNumberChange: (value: string) => void;
  disabled?: boolean;
  wrapperClassName?: string;
  selectClassName?: string;
  inputClassName?: string;
  ariaLabel?: string;
  searchPlaceholder?: string;
  items?: { value: string; label: string; keywords?: string }[];
}
export declare function PhoneDialField(props: PhoneDialFieldProps): ReactElement;
export default PhoneDialField;
