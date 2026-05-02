import type { SessionType } from '../types/api';

/** Visual badge props for displaying session type labels. */
interface SessionTypeBadgeProps {
  /** Session classification rendered in the badge. */
  sessionType: SessionType;
}

export function SessionTypeBadge({ sessionType }: SessionTypeBadgeProps) {
  return <span className={`badge ${sessionType.toLowerCase()}`}>{sessionType}</span>;
}

