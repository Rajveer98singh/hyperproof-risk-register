interface Props {
  message: string;
  onRetry?: () => void;
}

export function ErrorBanner({ message, onRetry }: Props) {
  return (
    <div className="error-banner" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button className="link-btn" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}
