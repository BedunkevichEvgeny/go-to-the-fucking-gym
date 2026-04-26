import type { SessionType } from '../types/api';

interface SessionTypeBadgeProps {
  sessionType: SessionType;
}

export function SessionTypeBadge({ sessionType }: SessionTypeBadgeProps) {
  return <span className={`badge ${sessionType.toLowerCase()}`}>{sessionType}</span>;
}

