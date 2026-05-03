import { useNavigate } from 'react-router-dom';
import { useAcceptProposal } from '../../hooks/useProfileGoalOnboarding';

type ProposalAcceptanceActionsProps = {
  proposalId: string;
  disabled?: boolean;
  onAccepted?: () => void | Promise<void>;
};

export function ProposalAcceptanceActions({ proposalId, disabled = false, onAccepted }: ProposalAcceptanceActionsProps) {
  const navigate = useNavigate();
  const acceptProposal = useAcceptProposal();

  const handleAccept = async () => {
    await acceptProposal.mutateAsync(proposalId);
    await onAccepted?.();
    navigate('/program-session');
  };

  return (
    <button className="button secondary" type="button" disabled={disabled || acceptProposal.isPending} onClick={handleAccept}>
      {acceptProposal.isPending ? 'Activating…' : 'Accept Plan'}
    </button>
  );
}

