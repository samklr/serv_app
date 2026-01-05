'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
    Calendar,
    Clock,
    MapPin,
    User,
    CheckCircle2,
    XCircle,
    AlertCircle,
    Star,
    DollarSign,
    Eye
} from 'lucide-react';
import { useAuthStore } from '@/lib/store';
import { providerApi, bookingApi, Booking } from '@/lib/api';
import { formatDate, formatCurrency, getBookingStatusLabel } from '@/lib/utils';
import Header from '@/components/Header';
import Footer from '@/components/Footer';

export default function ProviderDashboard() {
    const router = useRouter();
    const { user, isAuthenticated } = useAuthStore();
    const [bookings, setBookings] = useState<Booking[]>([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState<'pending' | 'active' | 'completed'>('pending');
    const [stats, setStats] = useState({
        pendingRequests: 0,
        activeBookings: 0,
        completedBookings: 0,
        totalEarnings: 0,
        averageRating: 0,
        profileViews: 0
    });

    useEffect(() => {
        if (!isAuthenticated) {
            router.push('/login');
            return;
        }

        if (user?.role !== 'PROVIDER') {
            router.push('/dashboard');
            return;
        }

        fetchBookings();
    }, [isAuthenticated, user, router]);

    const fetchBookings = async () => {
        try {
            setLoading(true);
            const data = await providerApi.getBookings();
            setBookings(data);

            // Calculate stats
            const pending = data.filter(b => b.status === 'PENDING').length;
            const active = data.filter(b => ['CONFIRMED', 'IN_PROGRESS'].includes(b.status)).length;
            const completed = data.filter(b => b.status === 'COMPLETED').length;
            const totalEarnings = data
                .filter(b => b.status === 'COMPLETED')
                .reduce((sum, b) => sum + (b.agreedPrice || 0), 0);

            setStats({
                pendingRequests: pending,
                activeBookings: active,
                completedBookings: completed,
                totalEarnings,
                averageRating: 4.8, // Mock for now
                profileViews: 127  // Mock for now
            });
        } catch (error) {
            console.error('Failed to fetch bookings:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleAcceptBooking = async (bookingId: string) => {
        try {
            await bookingApi.accept(bookingId);
            fetchBookings();
        } catch (error) {
            console.error('Failed to accept booking:', error);
        }
    };

    const handleDeclineBooking = async (bookingId: string) => {
        try {
            await bookingApi.decline(bookingId);
            fetchBookings();
        } catch (error) {
            console.error('Failed to decline booking:', error);
        }
    };

    const filteredBookings = bookings.filter(booking => {
        if (activeTab === 'pending') return booking.status === 'PENDING';
        if (activeTab === 'active') return ['CONFIRMED', 'IN_PROGRESS'].includes(booking.status);
        if (activeTab === 'completed') return ['COMPLETED', 'CANCELLED'].includes(booking.status);
        return true;
    });

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'PENDING': return <AlertCircle className="w-5 h-5 text-yellow-500" />;
            case 'CONFIRMED': return <CheckCircle2 className="w-5 h-5 text-blue-500" />;
            case 'IN_PROGRESS': return <Clock className="w-5 h-5 text-purple-500" />;
            case 'COMPLETED': return <CheckCircle2 className="w-5 h-5 text-green-500" />;
            case 'CANCELLED': return <XCircle className="w-5 h-5 text-red-500" />;
            default: return <AlertCircle className="w-5 h-5 text-gray-500" />;
        }
    };

    if (loading) {
        return (
            <>
                <Header />
                <main className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-purple-50 pt-24">
                    <div className="container mx-auto px-4 py-8">
                        <div className="flex items-center justify-center h-64">
                            <div className="spinner"></div>
                        </div>
                    </div>
                </main>
                <Footer />
            </>
        );
    }

    return (
        <>
            <Header />
            <main className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-purple-50 pt-24">
                <div className="container mx-auto px-4 py-8">
                    {/* Welcome Section */}
                    <div className="mb-8">
                        <h1 className="text-3xl font-bold text-gray-900">
                            Bonjour, {user?.firstName} 👋
                        </h1>
                        <p className="text-gray-600 mt-2">
                            Gérez vos demandes et suivez votre activité
                        </p>
                    </div>

                    {/* Stats Grid */}
                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
                        <div className="glass-effect rounded-xl p-4 text-center">
                            <div className="w-10 h-10 bg-yellow-100 rounded-lg flex items-center justify-center mx-auto mb-2">
                                <AlertCircle className="w-5 h-5 text-yellow-600" />
                            </div>
                            <p className="text-2xl font-bold text-gray-900">{stats.pendingRequests}</p>
                            <p className="text-sm text-gray-600">En attente</p>
                        </div>

                        <div className="glass-effect rounded-xl p-4 text-center">
                            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center mx-auto mb-2">
                                <Calendar className="w-5 h-5 text-blue-600" />
                            </div>
                            <p className="text-2xl font-bold text-gray-900">{stats.activeBookings}</p>
                            <p className="text-sm text-gray-600">En cours</p>
                        </div>

                        <div className="glass-effect rounded-xl p-4 text-center">
                            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center mx-auto mb-2">
                                <CheckCircle2 className="w-5 h-5 text-green-600" />
                            </div>
                            <p className="text-2xl font-bold text-gray-900">{stats.completedBookings}</p>
                            <p className="text-sm text-gray-600">Terminées</p>
                        </div>

                        <div className="glass-effect rounded-xl p-4 text-center">
                            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center mx-auto mb-2">
                                <DollarSign className="w-5 h-5 text-purple-600" />
                            </div>
                            <p className="text-2xl font-bold text-gray-900">{formatCurrency(stats.totalEarnings)}</p>
                            <p className="text-sm text-gray-600">Revenus</p>
                        </div>

                        <div className="glass-effect rounded-xl p-4 text-center">
                            <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center mx-auto mb-2">
                                <Star className="w-5 h-5 text-orange-600" />
                            </div>
                            <p className="text-2xl font-bold text-gray-900">{stats.averageRating}</p>
                            <p className="text-sm text-gray-600">Note moyenne</p>
                        </div>

                        <div className="glass-effect rounded-xl p-4 text-center">
                            <div className="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center mx-auto mb-2">
                                <Eye className="w-5 h-5 text-indigo-600" />
                            </div>
                            <p className="text-2xl font-bold text-gray-900">{stats.profileViews}</p>
                            <p className="text-sm text-gray-600">Vues profil</p>
                        </div>
                    </div>

                    {/* Quick Actions */}
                    <div className="flex flex-wrap gap-4 mb-8">
                        <Link
                            href="/provider/profile"
                            className="btn-secondary flex items-center gap-2"
                        >
                            <User className="w-4 h-4" />
                            Modifier mon profil
                        </Link>
                        <Link
                            href="/provider/availability"
                            className="btn-secondary flex items-center gap-2"
                        >
                            <Calendar className="w-4 h-4" />
                            Gérer mes disponibilités
                        </Link>
                    </div>

                    {/* Tabs */}
                    <div className="glass-effect rounded-xl overflow-hidden">
                        <div className="flex border-b border-gray-200">
                            <button
                                onClick={() => setActiveTab('pending')}
                                className={`flex-1 px-6 py-4 text-sm font-medium transition-colors ${activeTab === 'pending'
                                    ? 'text-purple-600 border-b-2 border-purple-600 bg-purple-50'
                                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                                    }`}
                            >
                                <span className="flex items-center justify-center gap-2">
                                    <AlertCircle className="w-4 h-4" />
                                    En attente ({stats.pendingRequests})
                                </span>
                            </button>
                            <button
                                onClick={() => setActiveTab('active')}
                                className={`flex-1 px-6 py-4 text-sm font-medium transition-colors ${activeTab === 'active'
                                    ? 'text-purple-600 border-b-2 border-purple-600 bg-purple-50'
                                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                                    }`}
                            >
                                <span className="flex items-center justify-center gap-2">
                                    <Clock className="w-4 h-4" />
                                    En cours ({stats.activeBookings})
                                </span>
                            </button>
                            <button
                                onClick={() => setActiveTab('completed')}
                                className={`flex-1 px-6 py-4 text-sm font-medium transition-colors ${activeTab === 'completed'
                                    ? 'text-purple-600 border-b-2 border-purple-600 bg-purple-50'
                                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                                    }`}
                            >
                                <span className="flex items-center justify-center gap-2">
                                    <CheckCircle2 className="w-4 h-4" />
                                    Historique ({stats.completedBookings})
                                </span>
                            </button>
                        </div>

                        {/* Bookings List */}
                        <div className="p-6">
                            {filteredBookings.length === 0 ? (
                                <div className="text-center py-12">
                                    <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                        <Calendar className="w-8 h-8 text-gray-400" />
                                    </div>
                                    <p className="text-gray-500">
                                        {activeTab === 'pending' && 'Aucune demande en attente'}
                                        {activeTab === 'active' && 'Aucune mission en cours'}
                                        {activeTab === 'completed' && 'Aucune mission terminée'}
                                    </p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {filteredBookings.map((booking) => (
                                        <div
                                            key={booking.id}
                                            className="border border-gray-200 rounded-xl p-6 hover:border-purple-300 transition-colors bg-white"
                                        >
                                            <div className="flex items-start justify-between mb-4">
                                                <div className="flex items-center gap-3">
                                                    {getStatusIcon(booking.status)}
                                                    <div>
                                                        <h3 className="font-semibold text-gray-900">
                                                            {booking.category?.name}
                                                        </h3>
                                                        <p className="text-sm text-gray-500">
                                                            Demande #{booking.id}
                                                        </p>
                                                    </div>
                                                </div>
                                                <span className={`px-3 py-1 rounded-full text-xs font-medium ${booking.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
                                                    booking.status === 'CONFIRMED' ? 'bg-blue-100 text-blue-700' :
                                                        booking.status === 'IN_PROGRESS' ? 'bg-purple-100 text-purple-700' :
                                                            booking.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                                                                'bg-red-100 text-red-700'
                                                    }`}>
                                                    {getBookingStatusLabel(booking.status)}
                                                </span>
                                            </div>

                                            <p className="text-gray-600 mb-4 line-clamp-2">
                                                {booking.description}
                                            </p>

                                            <div className="flex flex-wrap gap-4 text-sm text-gray-500 mb-4">
                                                <span className="flex items-center gap-1">
                                                    <User className="w-4 h-4" />
                                                    {booking.client?.name || 'Client'}
                                                </span>
                                                <span className="flex items-center gap-1">
                                                    <MapPin className="w-4 h-4" />
                                                    {booking.city}
                                                </span>
                                                {booking.preferredDate && (
                                                    <span className="flex items-center gap-1">
                                                        <Calendar className="w-4 h-4" />
                                                        {formatDate(booking.preferredDate)}
                                                    </span>
                                                )}
                                                {booking.agreedPrice && (
                                                    <span className="flex items-center gap-1">
                                                        <DollarSign className="w-4 h-4" />
                                                        {formatCurrency(booking.agreedPrice)}
                                                    </span>
                                                )}
                                            </div>

                                            {/* Actions */}
                                            < div className="flex items-center gap-3 pt-4 border-t border-gray-100" >
                                                {
                                                    booking.status === 'PENDING' && (
                                                        <>
                                                            <button
                                                                onClick={() => handleAcceptBooking(booking.id)}
                                                                className="btn-primary text-sm py-2 px-4"
                                                            >
                                                                Accepter
                                                            </button>
                                                            <button
                                                                onClick={() => handleDeclineBooking(booking.id)}
                                                                className="btn-secondary text-sm py-2 px-4"
                                                            >
                                                                Refuser
                                                            </button>
                                                        </>
                                                    )
                                                }
                                                < Link
                                                    href={`/provider/bookings/${booking.id}`}
                                                    className="text-purple-600 hover:text-purple-700 text-sm font-medium ml-auto"
                                                >
                                                    Voir les détails →
                                                </Link>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div >
                </div >
            </main >
            <Footer />
        </>
    );
}
