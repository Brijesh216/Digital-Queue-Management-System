const statusStyles = {
  ACTIVE: 'theme-status-active',
  PAUSED: 'theme-status-paused',
  CLOSED: 'theme-status-closed',
};

const statusLabels = {
  ACTIVE: 'Active',
  PAUSED: 'Queue Paused',
  CLOSED: 'Closed',
};

export default function QueueCard({ queue, joining, onJoin, actionLabel }) {
  const status = queue.status || 'UNKNOWN';
  const canJoin = status === 'ACTIVE' && !joining;

  return (
    <article className="theme-card group flex h-full flex-col justify-between rounded-2xl p-5 transition duration-200 hover:-translate-y-0.5 hover:border-cyan-700">
      <div>
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold tracking-tight text-slate-950">{queue.queueName}</h2>
            <p className="theme-muted mt-1 text-sm font-medium">
              {queue.serviceType}
            </p>
          </div>
          <span className={`rounded-full border px-3 py-1 text-[11px] font-bold uppercase tracking-wide ${statusStyles[status] || statusStyles.CLOSED}`}>
            {statusLabels[status] || status}
          </span>
        </div>

        <dl className="mt-6 grid grid-cols-2 gap-3">
          <div className="rounded-xl border border-[#1e3a52] bg-[#0b1728] p-3.5">
            <dt className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">Now serving</dt>
            <dd className="mt-1 text-2xl font-bold tracking-tight text-white">{queue.currentToken ?? '--'}</dd>
          </div>
          <div className="rounded-xl border border-[#1e3a52] bg-[#0b1728] p-3.5">
            <dt className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">Waiting</dt>
            <dd className="mt-1 text-2xl font-bold tracking-tight text-white">{queue.waitingCount ?? 0}</dd>
          </div>
        </dl>

        {queue.description && !/just testing/i.test(queue.description) && <p className="theme-muted mt-4 text-sm leading-6">{queue.description}</p>}
      </div>

      <button
        type="button"
        disabled={!canJoin}
        onClick={() => onJoin(queue.id)}
        className="theme-primary mt-6 w-full rounded-xl px-4 py-3 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:ring-offset-2 disabled:cursor-not-allowed"
      >
        {joining ? 'Joining...' : actionLabel || (status === 'ACTIVE' ? 'Join Queue' : statusLabels[status] || 'Unavailable')}
      </button>
    </article>
  );
}
