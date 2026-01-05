import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User } from './api';

interface AuthState {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    setAuth: (user: User, token: string) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            user: null,
            token: null,
            isAuthenticated: false,
            setAuth: (user, token) => {
                localStorage.setItem('token', token);
                set({ user, token, isAuthenticated: true });
            },
            logout: () => {
                localStorage.removeItem('token');
                set({ user: null, token: null, isAuthenticated: false });
            },
        }),
        {
            name: 'auth-storage',
            partialize: (state) => ({ user: state.user, token: state.token, isAuthenticated: state.isAuthenticated }),
        }
    )
);

// Booking wizard store
interface BookingWizardState {
    step: number;
    categoryId: string | null;
    categoryName: string | null;
    postalCode: string;
    city: string;
    canton: string;
    description: string;
    preferredDate: string | null;
    preferredTimeSlot: string | null;
    selectedProviderId: string | null;
    selectedProviderName: string | null;
    budgetMin: number | null;
    budgetMax: number | null;
    setStep: (step: number) => void;
    setCategory: (id: string, name: string) => void;
    setLocation: (postalCode: string, city: string, canton: string) => void;
    setDescription: (description: string) => void;
    setPreferredTime: (date: string | null, timeSlot: string | null) => void;
    setSelectedProvider: (id: string | null, name: string | null) => void;
    setBudget: (min: number | null, max: number | null) => void;
    reset: () => void;
}

const initialBookingState = {
    step: 0,
    categoryId: null,
    categoryName: null,
    postalCode: '',
    city: '',
    canton: 'JU',
    description: '',
    preferredDate: null,
    preferredTimeSlot: null,
    selectedProviderId: null,
    selectedProviderName: null,
    budgetMin: null,
    budgetMax: null,
};

export const useBookingWizardStore = create<BookingWizardState>((set) => ({
    ...initialBookingState,
    setStep: (step) => set({ step }),
    setCategory: (categoryId, categoryName) => set({ categoryId, categoryName }),
    setLocation: (postalCode, city, canton) => set({ postalCode, city, canton }),
    setDescription: (description) => set({ description }),
    setPreferredTime: (preferredDate, preferredTimeSlot) => set({ preferredDate, preferredTimeSlot }),
    setSelectedProvider: (selectedProviderId, selectedProviderName) => set({ selectedProviderId, selectedProviderName }),
    setBudget: (budgetMin, budgetMax) => set({ budgetMin, budgetMax }),
    reset: () => set(initialBookingState),
}));
