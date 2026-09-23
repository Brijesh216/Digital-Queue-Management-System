import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import QueueCard from '../components/QueueCard';
import queueService from '../services/queueService';
import websocketService from '../services/websocketService';

const unwrapError = (error, fallback) =>
  error.response?.data?.message || error.response?.data?.error || fallback;

const tokenLabel = (token) => token?.displayToken || token?.tokenNumber || '--';

const emptyQueueForm = {
  queueName: '',
  serviceType: '',
  queueCode: '',
  description: '',
  averageServiceTime: '',
};

export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const [queues, setQueues] = useState([]);
  const [selectedQueueId, setSelectedQueueId] = useState(null);
  const [waitingTokens, setWaitingTokens] = useState([]);
  const [activeToken, setActiveToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createForm, setCreateForm] = useState(emptyQueueForm);
  const [createErrors, setCreateErrors] = useState({});
  const [creating, setCreating] = useState(false);

  const selectedQueue = queues.find((queue) => queue.id === selectedQueueId) || null;

  const refreshQueueDetails = async (queueId) => {
    const [queue, waiting, active] = await Promise.all([
      queueService.getQueue(queueId),
      queueService.getWaitingTokens(queueId),
      queueService.getActiveToken(queueId),
    ]);

    setQueues((currentQueues) => currentQueues.map((item) => item.id === queueId ? queue : item));
    setWaitingTokens(waiting || []);
    setActiveToken(active || null);
  };

  useEffect(() => {
    let mounted = true;

    const handleQueueUpdate = (update) => {
      if (!mounted || !update.queueId) return;
      setQueues((currentQueues) => currentQueues.map((queue) => (
        queue.id === update.queueId
          ? { ...queue, currentToken: update.currentToken, waitingCount: update.waitingCount, status: update.queueStatus }
          : queue
      )));
      if (update.queueId === selectedQueueId) {
        refreshQueueDetails(update.queueId).catch(() => {});
      }
    };

    websocketService.connect(handleQueueUpdate);

    queueService.getQueues()
      .then((availableQueues) => {
        if (!mounted) return;
        setQueues(availableQueues);
        availableQueues.forEach((queue) => websocketService.subscribe(queue.id));
        if (availableQueues.length > 0) setSelectedQueueId(availableQueues[0].id);
      })
      .catch((loadError) => {
        if (mounted) setError(unwrapError(loadError, 'Unable to load queues.'));
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
      websocketService.disconnect();
    };
  }, []);

  useEffect(() => {
    if (selectedQueueId) {
      refreshQueueDetails(selectedQueueId).catch((loadError) => setError(unwrapError(loadError, 'Unable to load queue details.')));
    }
  }, [selectedQueueId]);

  const runAction = async (actionName, request, successMessage) => {
    if (!selectedQueueId) return;
    setAction(actionName);
    setMessage(null);
    setError('');
    try {
      await request();
      await refreshQueueDetails(selectedQueueId);
      setMessage(successMessage);
    } catch (actionError) {
      setError(unwrapError(actionError, 'The queue action could not be completed.'));
    } finally {
      setAction('');
    }
  };

  const callNext = () => runAction('next', () => queueService.callNextToken(selectedQueueId), 'Next waiting token called.');
  const completeCurrent = () => activeToken && runAction('complete', () => queueService.completeToken(activeToken.id), 'Current token completed.');
  const skipWaiting = (tokenId) => runAction(`skip-${tokenId}`, () => queueService.skipToken(tokenId), 'Waiting token skipped.');
  const pause = () => runAction('pause', () => queueService.pauseQueue(selectedQueueId), 'Queue paused.');
  const resume = () => runAction('resume', () => queueService.resumeQueue(selectedQueueId), 'Queue resumed.');

  const validateCreateForm = () => {
    const errors = {};
    if (!createForm.queueName.trim()) errors.queueName = 'Queue name is required.';
    if (!createForm.serviceType.trim()) errors.serviceType = 'Service type is required.';
    if (!createForm.queueCode.trim()) errors.queueCode = 'Queue code is required.';
    if (createForm.averageServiceTime !== '' && (!Number.isInteger(Number(createForm.averageServiceTime)) || Number(createForm.averageServiceTime) < 0)) {
      errors.averageServiceTime = 'Enter a whole number of minutes.';
    }
    return errors;
  };

  const handleCreateQueue = async (event) => {
    event.preventDefault();
    const validationErrors = validateCreateForm();
    setCreateErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    setCreating(true);
    setError('');
    setMessage(null);
    try {
      const createdQueue = await queueService.createQueue({
        queueName: createForm.queueName.trim(),
        serviceType: createForm.serviceType.trim(),
        queueCode: createForm.queueCode.trim(),
        description: createForm.description.trim() || null,
        averageServiceTime: createForm.averageServiceTime === '' ? null : Number(createForm.averageServiceTime),
      });
      setQueues((currentQueues) => [createdQueue, ...currentQueues]);
      setSelectedQueueId(createdQueue.id);
      websocketService.subscribe(createdQueue.id);
      setCreateForm(emptyQueueForm);
      setCreateErrors({});
      setShowCreateForm(false);
      setMessage('Queue created successfully.');
    } catch (createError) {
      setError(unwrapError(createError, 'Unable to create queue. Queue code may already exist.'));
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="theme-dark min-h-screen px-4 py-8 sm:px-6 lg:px-8">
      <main className="mx-auto max-w-7xl">
        <header className="theme-grid-line flex flex-col justify-between gap-5 border-b pb-8 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-700">Operations</p>
            <h1 className="mt-2 text-4xl font-bold tracking-tight text-slate-950">Admin Dashboard</h1>
            <p className="mt-2 text-slate-600">Signed in as {user?.username || 'operator'}.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button onClick={() => { setShowCreateForm(true); setError(''); setMessage(null); }} className="theme-primary rounded-xl px-4 py-2.5 text-sm font-semibold">Create Queue</button>
            <button onClick={logout} className="theme-secondary rounded-xl px-4 py-2.5 text-sm font-semibold">Log out</button>
          </div>
        </header>

        {message && <div role="status" className="theme-success mt-6 rounded-xl border px-4 py-3 text-sm font-medium">{message}</div>}
        {error && <div role="alert" className="theme-danger mt-6 rounded-xl border px-4 py-3 text-sm font-medium">{error}</div>}

        {showCreateForm && (
          <div className="fixed inset-0 z-20 flex items-center justify-center bg-[#020817]/80 px-4 py-8" role="dialog" aria-modal="true" aria-labelledby="create-queue-title">
            <form onSubmit={handleCreateQueue} className="theme-modal max-h-full w-full max-w-xl overflow-y-auto rounded-2xl p-6">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h2 id="create-queue-title" className="text-2xl font-bold text-slate-950">Create Queue</h2>
                  <p className="mt-1 text-sm text-slate-500">New queues start as ACTIVE.</p>
                </div>
                <button type="button" onClick={() => setShowCreateForm(false)} className="text-2xl leading-none text-slate-400 hover:text-slate-700" aria-label="Close">×</button>
              </div>

              <div className="mt-6 grid gap-4 sm:grid-cols-2">
                {[
                  ['queueName', 'Queue name', 'Counter 1'],
                  ['serviceType', 'Service type', 'BANKING'],
                  ['queueCode', 'Queue code', 'BANK_001'],
                  ['averageServiceTime', 'Average service time (minutes)', '5'],
                ].map(([field, label, placeholder]) => (
                  <label key={field} className="text-sm font-medium text-slate-700">
                    {label}
                    <input
                      value={createForm[field]}
                      onChange={(event) => setCreateForm({ ...createForm, [field]: event.target.value })}
                      placeholder={placeholder}
                      type={field === 'averageServiceTime' ? 'number' : 'text'}
                      min={field === 'averageServiceTime' ? '0' : undefined}
                      className="theme-input mt-1 w-full rounded-xl border px-3 py-2.5 font-normal outline-none"
                    />
                    {createErrors[field] && <span className="mt-1 block text-xs font-normal text-rose-600">{createErrors[field]}</span>}
                  </label>
                ))}
                <label className="text-sm font-medium text-slate-700 sm:col-span-2">
                  Description
                  <textarea value={createForm.description} onChange={(event) => setCreateForm({ ...createForm, description: event.target.value })} maxLength={255} rows={3} className="theme-input mt-1 w-full rounded-xl border px-3 py-2.5 font-normal outline-none" />
                </label>
              </div>

              <div className="mt-6 flex justify-end gap-2">
                <button type="button" onClick={() => setShowCreateForm(false)} className="theme-secondary rounded-xl px-4 py-2.5 text-sm font-semibold">Cancel</button>
                <button type="submit" disabled={creating} className="theme-primary rounded-xl px-4 py-2.5 text-sm font-semibold disabled:cursor-not-allowed">{creating ? 'Creating...' : 'Create Queue'}</button>
              </div>
            </form>
          </div>
        )}

        {loading ? <div className="mt-8 h-64 animate-pulse rounded-2xl bg-white" /> : queues.length === 0 ? (
          <div className="theme-card mt-8 rounded-2xl border-dashed px-6 py-14 text-center">No queues are available.</div>
        ) : (
          <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
            <section>
              <div className="grid gap-5 md:grid-cols-2">
                {queues.map((queue) => <QueueCard key={queue.id} queue={queue} joining={false} actionLabel="Manage Queue" onJoin={() => setSelectedQueueId(queue.id)} />)}
              </div>
            </section>

            <aside className="theme-aside h-fit rounded-2xl p-5">
              <label htmlFor="queue-select" className="text-xs font-semibold uppercase tracking-wide text-slate-500">Manage queue</label>
              <select id="queue-select" value={selectedQueueId || ''} onChange={(event) => setSelectedQueueId(Number(event.target.value))} className="theme-input mt-2 w-full rounded-xl border px-3 py-2.5 text-sm font-medium">
                {queues.map((queue) => <option key={queue.id} value={queue.id}>{queue.queueName}</option>)}
              </select>

              <div className="mt-5 rounded-xl border border-[#1e3a52] bg-[#07111f] p-4 text-white">
                <p className="text-xs uppercase tracking-wide text-slate-400">Current serving</p>
                <p className="mt-1 text-4xl font-bold">{tokenLabel(activeToken)}</p>
                <p className="mt-1 text-sm text-slate-300">{selectedQueue?.status || '--'} · {selectedQueue?.waitingCount ?? 0} waiting</p>
              </div>

              <div className="mt-5 grid gap-2">
                <button onClick={callNext} disabled={action || selectedQueue?.status !== 'ACTIVE'} className="theme-primary rounded-xl px-4 py-3 text-sm font-semibold disabled:cursor-not-allowed">{action === 'next' ? 'Calling...' : 'Call Next Token'}</button>
                <button onClick={completeCurrent} disabled={action || !activeToken} className="theme-secondary rounded-xl px-4 py-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50">{action === 'complete' ? 'Completing...' : 'Complete Current Token'}</button>
                {selectedQueue?.status === 'PAUSED' ? (
                  <button onClick={resume} disabled={Boolean(action)} className="theme-success rounded-xl px-4 py-3 text-sm font-semibold disabled:opacity-50">{action === 'resume' ? 'Resuming...' : 'Resume Queue'}</button>
                ) : (
                  <button onClick={pause} disabled={Boolean(action) || selectedQueue?.status !== 'ACTIVE'} className="theme-warning rounded-xl px-4 py-3 text-sm font-semibold disabled:opacity-50">{action === 'pause' ? 'Pausing...' : 'Pause Queue'}</button>
                )}
              </div>

              <div className="mt-6 border-t border-slate-200 pt-5">
                <h2 className="text-sm font-semibold text-slate-900">Waiting tokens</h2>
                <div className="mt-3 space-y-2">
                  {waitingTokens.length === 0 ? <p className="text-sm text-slate-500">No waiting tokens.</p> : waitingTokens.map((token) => (
                    <div key={token.id} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2">
                      <span className="text-sm font-semibold text-slate-800">{tokenLabel(token)}</span>
                      <button onClick={() => skipWaiting(token.id)} disabled={Boolean(action)} className="text-xs font-semibold text-rose-600 hover:text-rose-800 disabled:opacity-50">{action === `skip-${token.id}` ? 'Skipping...' : 'Skip'}</button>
                    </div>
                  ))}
                </div>
              </div>
            </aside>
          </div>
        )}
      </main>
    </div>
  );
}
