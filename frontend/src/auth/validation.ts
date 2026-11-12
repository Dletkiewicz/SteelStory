export function validPassword(password: string, registration: boolean): boolean {
  const length = Array.from(password).length;
  return !/[\uD800-\uDFFF]/u.test(password) && length >= (registration ? 15 : 1) && length <= 128;
}
