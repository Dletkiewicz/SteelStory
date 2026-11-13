export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string) {
    super(code);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

const messages: Record<string, string> = {
  CHARACTER_NAME_TAKEN: 'Ta nazwa jest już zajęta. Wybierz inną nazwę postaci.',
  CHARACTER_LIMIT_REACHED: 'Masz już trzy postacie. Wróć do wyboru postaci, aby odświeżyć listę.',
  INVALID_CHARACTER_INPUT: 'Sprawdź nazwę, klasę i przydział dokładnie 10 punktów.',
  EMAIL_ALREADY_REGISTERED: 'Konto z tym adresem już istnieje. Przejdź do logowania.',
  INVALID_CREDENTIALS: 'Nieprawidłowy adres e-mail lub hasło.',
  INVALID_IDENTITY_INPUT: 'Sprawdź adres e-mail i wymagania dotyczące hasła.',
};

export function errorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return 'Nie udało się wykonać operacji. Spróbuj ponownie.';
  const message = messages[error.code];
  if (message) return message;
  if (error.status === 401) return 'Sesja wygasła. Zaloguj się ponownie.';
  if (error.status === 403) return 'Nie można zatwierdzić żądania. Odśwież sesję i spróbuj ponownie.';
  if (error.status === 404) return 'Nie znaleziono tej postaci na Twoim koncie.';
  if (error.status === 400) return 'Sprawdź wprowadzone dane i spróbuj ponownie.';
  if (error.status === 413) return 'Wprowadzone dane są zbyt długie.';
  if (error.status === 0) return 'Brak odpowiedzi z serwera. Sprawdź połączenie i spróbuj ponownie.';
  return 'Serwer jest chwilowo niedostępny. Spróbuj ponownie za chwilę.';
}
