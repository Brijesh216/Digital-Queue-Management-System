import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import QueueCard from '../components/QueueCard';
import queueService from '../services/queueService';
import websocketService from '../services/websocketService';

const getErrorMessage = (error, fallback) =>
  error.response?.data?.message || error.response?.data?.error || fallback;

export default function Dashboard() {
  const { user, logout } = useAuth();
  const [queues, setQueues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [joiningQueueId, setJoiningQueueId] = useState(null);
  const [error, setError] = useState('');
  const [joinedToken, setJoinedToken] = useState(null);

  const activeQueue = joinedToken
    ? queues.find((queue) => queue.id === joinedToken.queue?.id)
    : null;
  const isBeingServed = ['CALLED', 'SERVING'].includes(joinedToken?.status);

  useEffect(() => {
    let mounted = true;

    const handleQueueUpdate = (update) => {
      if (!mounted || !update.queueId) return;

      setQueues((currentQueues) => currentQueues.map((queue) => (
        queue.id === update.queueId
          ? {
              ...queue,
              currentToken: update.currentToken,
              waitingCount: update.waitingCount,
              status: update.queueStatus,
            }
          : queue
      )));
    };

    websocketService.connect(handleQueueUpdate);

    const loadQueues = async () => {
      try {
        const availableQueues = await queueService.getQueues();
        if (!mounted) return;

        setQueues(availableQueues);
        availableQueues.forEach((queue) => websocketService.subscribe(queue.id));
      } catch (loadError) {
        if (mounted) setError(getErrorMessage(loadError, 'Unable to load queues.'));
      } finally {
        if (mounted) setLoading(false);
      }
    };

    loadQueues();

    return () => {
      mounted = false;
      websocketService.disconnect();
    };
  }, []);

  const handleJoinQueue = async (queueId) => {
    setJoiningQueueId(queueId);
    setError('');

    try {
      const token = await queueService.joinQueue(queueId);
      setJoinedToken(token);
      const refreshedQueues = await queueService.getQueues();
      setQueues(refreshedQueues);
      refreshedQueues.forEach((queue) => websocketService.subscribe(queue.id));
    } catch (joinError) {
      setError(getErrorMessage(joinError, 'Unable to join this queue.'));
    } finally {
      setJoiningQueueId(null);
    }
  };

  return (
    <div className="theme-dark min-h-screen px-4 py-6 text-slate-900 sm:px-6 lg:px-8 lg:py-8">
      <main className="mx-auto max-w-6xl">
        <header className="theme-grid-line flex flex-col justify-between gap-6 border-b pb-7 sm:flex-row sm:items-center">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-cyan-800 text-sm font-black tracking-tight text-white shadow-lg shadow-cyan-900/15">DQ</div>
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.22em] text-cyan-800">Digital Queue</p>
              <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">Dashboard <span className="font-normal text-slate-400">/</span> Choose a Service</h1>
            </div>
          </div>
          <div className="flex items-center justify-between gap-4 sm:justify-end">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-cyan-100 text-sm font-bold text-cyan-800">
                {(user?.firstName || user?.username || 'C').charAt(0).toUpperCase()}
              </div>
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Signed in as</p>
                <p className="truncate text-sm font-bold text-slate-800">{user?.firstName || user?.username || 'Customer'}</p>
              </div>
            </div>
            <button onClick={logout} className="rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-400 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:ring-offset-2">
              Log out
            </button>
          </div>
        </header>

        {joinedToken && (
          <section className="mt-8 overflow-hidden rounded-3xl bg-slate-950 text-white shadow-[0_18px_45px_rgba(15,23,42,0.16)]">
            <div className="border-b border-white/10 px-6 py-5 sm:px-8">
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-cyan-300">Your Queue</p>
              <div className="mt-2 flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
                <div>
                  <h2 className="text-2xl font-bold tracking-tight">{joinedToken.queue?.queueName || 'Your selected queue'}</h2>
                  <p className="mt-1 text-sm text-slate-400">{joinedToken.queue?.serviceType || 'Service queue'}</p>
                </div>
                <span className={`w-fit rounded-full px-3 py-1 text-xs font-bold uppercase tracking-wide ${isBeingServed ? 'bg-cyan-400 text-white' : 'bg-white/10 text-slate-200'}`}>
                  {isBeingServed ? 'Now serving you' : joinedToken.status || 'WAITING'}
                </span>
              </div>
            </div>
            <div className="grid gap-px bg-white/10 sm:grid-cols-4">
              <div className="bg-slate-950 px-6 py-6 sm:px-8">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Your token</p>
                <p className="mt-2 text-5xl font-black tracking-tight text-cyan-300">{joinedToken.displayToken || joinedToken.tokenNumber}</p>
              </div>
              <div className="bg-slate-950 px-6 py-6 sm:px-5">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Current token</p>
                <p className="mt-2 text-3xl font-bold tracking-tight">{activeQueue?.currentToken ?? '--'}</p>
              </div>
              <div className="bg-slate-950 px-6 py-6 sm:px-5">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">People ahead</p>
                <p className="mt-2 text-3xl font-bold tracking-tight">{activeQueue?.waitingCount ?? '--'}</p>
              </div>
              <div className="bg-slate-950 px-6 py-6 sm:px-5">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Estimated wait</p>
                <p className="mt-2 text-3xl font-bold tracking-tight">{joinedToken.estimatedWaitTime ?? '--'} <span className="text-base font-medium text-slate-400">min</span></p>
              </div>
            </div>
          </section>
        )}

        {error && <div role="alert" className="theme-danger mt-6 rounded-2xl border px-4 py-3 text-sm font-medium">{error}</div>}

        <section className="mt-10">
          <div className="mb-5 flex items-end justify-between gap-4">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-cyan-800">Services</p>
              <h2 className="mt-1 text-2xl font-bold tracking-tight text-slate-950">Available Services</h2>
            </div>
            {!loading && queues.length > 0 && <p className="text-sm text-slate-500">{queues.length} {queues.length === 1 ? 'service' : 'services'} available</p>}
          </div>
          {loading ? (
            <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
              {[1, 2, 3].map((item) => <div key={item} className="theme-card h-72 animate-pulse rounded-2xl" />)}
            </div>
          ) : queues.length === 0 ? (
            <div className="theme-card rounded-2xl border-dashed px-6 py-16 text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-2xl text-slate-400">—</div>
              <h3 className="mt-5 text-lg font-bold text-slate-900">No services available</h3>
              <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-500">There are currently no active queues. Please check again later.</p>
            </div>
          ) : (
            <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
              {queues.map((queue) => (
                <QueueCard key={queue.id} queue={queue} joining={joiningQueueId === queue.id} onJoin={handleJoinQueue} />
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
