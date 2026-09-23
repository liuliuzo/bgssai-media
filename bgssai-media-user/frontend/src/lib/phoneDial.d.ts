export declare const DEFAULT_DIAL_CODE: '+86';
export interface DialCode { dial: string; label: string; keywords?: string }
export interface PhoneValue { dial: string; national: string }
export declare const DIAL_CODES: DialCode[];
export declare function digitsOnly(value: unknown): string;
export declare function composeApiPhone(dialCode: string, nationalNumber: string, format?: 'cn-national' | 'e164'): string;
export declare function validatePhoneParts(dialCode: string, nationalNumber: string): string | null;
export declare function nationalMaxLength(dialCode: string): number;
export declare function nationalPlaceholder(dialCode: string): string;
export declare function emptyPhoneValue(): PhoneValue;
export declare function composePhoneValue(value?: PhoneValue, format?: 'cn-national' | 'e164'): string;
export declare function toDialItems(codes?: DialCode[]): { value: string; label: string; keywords: string }[];
