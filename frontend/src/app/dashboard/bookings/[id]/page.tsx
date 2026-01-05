'use client';

import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import { useAuthStore } from '@/lib/store';
import { bookingApi, Booking, Message } from '@/lib/api';
import {
    formatDate,
    formatDateTime,
    getBookingStatusLabel,
    getBookingStatusColor,
    getCategoryIcon,
    formatCurrency
} from '@/lib/utils';
import {
    ArrowLeft,
    Send,
    Star,
    MapPin,
    Calendar,
    Clock,
    Shield,
    Loader2,
    CheckCircle,
    XCircle,
    MessageCircle
} from 'lucide-react';

export default function BookingDetailPage({ params }: { params: Promise<{ id: string }> }) {
    const router = useRouter();
    const { user, isAuthenticated } = useAuthStore();
    const [booking, setBooking] = useState<Booking | null>(null);
    const [messages, setMessages] = useState<Message[]>([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [sendingMessage, setSendingMessage] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const [bookingId, setBookingId] = useState<string>('');

    useEffect(() => {
        async function getParams() {
            const resolvedParams = await params;
            setBookingId(resolvedParams.id);
        }
        getParams();
    }, [params]);

    useEffect(() => {
        if (!isAuthenticated) {
            router.push('/login');
            return;
        }
        if (bookingId) {
            fetchBookingDetails();
        }
    }, [isAuthenticated, bookingId]);

    const fetchBookingDetails = async () => {
        try {
            const [bookingData, messagesData] = await Promise.all([
                bookingApi.getById(bookingId),
                bookingApi.getMessages(bookingId),
            ]);
            setBooking(bookingData);
            setMessages(messagesData);
        } catch (error) {
            console.error('Failed to fetch booking:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSendMessage = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newMessage.trim() || sendingMessage) return;

        setSendingMessage(true);
        try {
            const message = await bookingApi.sendMessage(bookingId, newMessage.trim());
            setMessages([...messages, message]);
            setNewMessage('');
            setTimeout(() => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 100);
        } catch (error) {
            console.error('Failed to send message:', error);
        } finally {
            setSendingMessage(false);
        }
    };

    const handleCancel = async () => {
        if (!confirm('Êtes-vous sûr de vouloir annuler cette demande ?')) return;

        setActionLoading(true);
        try {
            const updated = await bookingApi.cancel(bookingId);
            setBooking(updated);
        } catch (error) {
            console.error('Failed to cancel:', error);
        } finally {
            setActionLoading(false);
        }
    };

    if (!isAuthenticated) return null;

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50">
                <Header />
                <div className="flex items-center justify-center pt-32">
                    <div className="spinner"></div>
                </div>
            </div>
        );
    }

    if (!booking) {
        return (
            <div className="min-h-screen bg-gray-50">
                <Header />
                <div className="pt-24 text-center">
                    <h1 className="text-xl font-semibold text-gray-900">Demande non trouvée</h1>
                    <Link href="/dashboard" className="text-violet-600 hover:underline mt-4 inline-block">
                        Retour au tableau de bord
                    </Link>
                </div>
            </div>
        );
    }

    const isActive = ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS'].includes(booking.status);

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="pt-20 pb-12">
                <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
                    {/* Back Link */}
                    <Link
                        href="/dashboard"
                        className="inline-flex items-center gap-2 text-gray-600 hover:text-violet-600 mb-6 transition-colors"
                    >
                        <ArrowLeft className="w-4 h-4" />
                        Retour au tableau de bord
                    </Link>

                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Main Content */}
                        <div className="lg:col-span-2 space-y-6">
                            {/* Booking Info Card */}
                            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                                <div className="flex items-start gap-4 mb-6">
                                    <div className="w-14 h-14 rounded-xl bg-violet-100 flex items-center justify-center text-3xl">
                                        {getCategoryIcon(booking.category.slug)}
                                    </div>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 flex-wrap">
                                            <h1 className="text-xl font-bold text-gray-900">{booking.category.name}</h1>
                                            <span className={`px-3 py-1 rounded-full text-sm font-medium ${getBookingStatusColor(booking.status)}`}>
                                                {getBookingStatusLabel(booking.status)}
                                            </span>
                                        </div>
                                        <p className="text-gray-500 mt-1">Demande du {formatDate(booking.createdAt)}</p>
                                    </div>
                                </div>

                                <div className="space-y-4">
                                    <div>
                                        <h3 className="text-sm font-medium text-gray-500 mb-1">Description</h3>
                                        <p className="text-gray-700">{booking.description}</p>
                                    </div>

                                    <div className="grid grid-cols-2 gap-4">
                                        <div className="flex items-center gap-2 text-gray-600">
                                            <MapPin className="w-5 h-5 text-gray-400" />
                                            <span>{booking.postalCode} {booking.city}</span>
                                        </div>
                                        {booking.preferredDate && (
                                            <div className="flex items-center gap-2 text-gray-600">
                                                <Calendar className="w-5 h-5 text-gray-400" />
                                                <span>{formatDate(booking.preferredDate)}</span>
                                            </div>
                                        )}
                                        {booking.preferredTimeSlot && (
                                            <div className="flex items-center gap-2 text-gray-600">
                                                <Clock className="w-5 h-5 text-gray-400" />
                                                <span>{booking.preferredTimeSlot}</span>
                                            </div>
                                        )}
                                    </div>

                                    {(booking.budgetMin || booking.budgetMax) && (
                                        <div className="pt-4 border-t">
                                            <span className="text-gray-500">Budget :</span>
                                            <span className="ml-2 font-semibold">
                                                {booking.budgetMin && formatCurrency(booking.budgetMin)}
                                                {booking.budgetMin && booking.budgetMax && ' - '}
                                                {booking.budgetMax && formatCurrency(booking.budgetMax)}
                                            </span>
                                        </div>
                                    )}
                                </div>

                                {isActive && (
                                    <div className="mt-6 pt-6 border-t">
                                        <button
                                            onClick={handleCancel}
                                            disabled={actionLoading}
                                            className="px-4 py-2 rounded-lg border border-red-200 text-red-600 hover:bg-red-50 transition-colors text-sm font-medium disabled:opacity-50"
                                        >
                                            {actionLoading ? 'Annulation...' : 'Annuler la demande'}
                                        </button>
                                    </div>
                                )}
                            </div>

                            {/* Messages Section */}
                            {booking.provider && (
                                <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
                                    <div className="p-4 border-b flex items-center gap-2">
                                        <MessageCircle className="w-5 h-5 text-violet-500" />
                                        <h2 className="font-semibold text-gray-900">Messages</h2>
                                    </div>

                                    <div className="h-[400px] overflow-y-auto p-4 space-y-4">
                                        {messages.length === 0 ? (
                                            <p className="text-center text-gray-500 py-8">
                                                Aucun message. Commencez la conversation !
                                            </p>
                                        ) : (
                                            messages.map((msg) => (
                                                <div
                                                    key={msg.id}
                                                    className={`flex ${msg.senderId === user?.id ? 'justify-end' : 'justify-start'}`}
                                                >
                                                    <div
                                                        className={`max-w-[80%] rounded-2xl px-4 py-3 ${msg.senderId === user?.id
                                                                ? 'bg-violet-600 text-white rounded-br-none'
                                                                : 'bg-gray-100 text-gray-900 rounded-bl-none'
                                                            }`}
                                                    >
                                                        <p>{msg.content}</p>
                                                        <p className={`text-xs mt-1 ${msg.senderId === user?.id ? 'text-violet-200' : 'text-gray-400'
                                                            }`}>
                                                            {formatDateTime(msg.createdAt)}
                                                        </p>
                                                    </div>
                                                </div>
                                            ))
                                        )}
                                        <div ref={messagesEndRef} />
                                    </div>

                                    <form onSubmit={handleSendMessage} className="p-4 border-t">
                                        <div className="flex gap-2">
                                            <input
                                                type="text"
                                                value={newMessage}
                                                onChange={(e) => setNewMessage(e.target.value)}
                                                placeholder="Écrivez votre message..."
                                                className="flex-1 px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
                                            />
                                            <button
                                                type="submit"
                                                disabled={!newMessage.trim() || sendingMessage}
                                                className="px-4 py-3 rounded-xl btn-primary disabled:opacity-50"
                                            >
                                                {sendingMessage ? (
                                                    <Loader2 className="w-5 h-5 animate-spin" />
                                                ) : (
                                                    <Send className="w-5 h-5" />
                                                )}
                                            </button>
                                        </div>
                                    </form>
                                </div>
                            )}
                        </div>

                        {/* Sidebar */}
                        <div className="space-y-6">
                            {/* Provider Card */}
                            {booking.provider ? (
                                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                                    <h2 className="font-semibold text-gray-900 mb-4">Votre prestataire</h2>
                                    <div className="flex items-center gap-4 mb-4">
                                        <div className="w-16 h-16 rounded-full bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white text-2xl font-semibold">
                                            {booking.provider.name.charAt(0)}
                                        </div>
                                        <div>
                                            <h3 className="font-semibold text-gray-900">{booking.provider.name}</h3>
                                            {booking.provider.isVerified && (
                                                <span className="verified-badge mt-1 inline-flex">
                                                    <Shield className="w-3 h-3" />
                                                    Vérifié
                                                </span>
                                            )}
                                            {booking.provider.averageRating && (
                                                <div className="flex items-center gap-1 mt-1">
                                                    <Star className="w-4 h-4 text-yellow-500 fill-yellow-500" />
                                                    <span className="text-sm text-gray-600">
                                                        {booking.provider.averageRating.toFixed(1)}
                                                    </span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                                    <h2 className="font-semibold text-gray-900 mb-2">En attente d'un prestataire</h2>
                                    <p className="text-sm text-gray-500">
                                        Votre demande est en cours de traitement. Vous serez notifié lorsqu'un prestataire l'acceptera.
                                    </p>
                                </div>
                            )}

                            {/* Status Timeline */}
                            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                                <h2 className="font-semibold text-gray-900 mb-4">Historique</h2>
                                <div className="space-y-4">
                                    {[
                                        { status: 'REQUESTED', date: booking.createdAt, label: 'Demande créée' },
                                        booking.status !== 'REQUESTED' && { status: booking.status, date: booking.updatedAt, label: getBookingStatusLabel(booking.status) },
                                    ].filter(Boolean).map((item: any, i) => (
                                        <div key={i} className="flex items-start gap-3">
                                            <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${item.status === 'COMPLETED' ? 'bg-green-100 text-green-600' :
                                                    item.status === 'CANCELED' || item.status === 'DECLINED' ? 'bg-red-100 text-red-600' :
                                                        'bg-violet-100 text-violet-600'
                                                }`}>
                                                {item.status === 'COMPLETED' ? <CheckCircle className="w-4 h-4" /> :
                                                    item.status === 'CANCELED' || item.status === 'DECLINED' ? <XCircle className="w-4 h-4" /> :
                                                        <Clock className="w-4 h-4" />}
                                            </div>
                                            <div>
                                                <p className="font-medium text-gray-900">{item.label}</p>
                                                <p className="text-sm text-gray-500">{formatDateTime(item.date)}</p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
