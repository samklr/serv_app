import axios, { AxiosInstance, AxiosError } from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Create axios instance
const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Types
export interface User {
  id: string;
  email: string;
  name: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  role: 'CLIENT' | 'PROVIDER' | 'ADMIN';
  hasProviderProfile: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Category {
  id: string;
  slug: string;
  name: string;
  description: string;
  icon: string;
  sortOrder: number;
}

export interface ProviderMatch {
  id: string;
  userId: string;
  name: string;
  photoUrl: string | null;
  bio: string;
  languages: string[];
  isVerified: boolean;
  averageRating: number | null;
  ratingCount: number;
  city: string;
  hourlyRate: number | null;
  fixedPrice: number | null;
  pricingType: string;
  responseTimeMinutes: number | null;
}

export interface Booking {
  id: string;
  category: { id: string; slug: string; name: string };
  client: { id: string; name: string; email: string };
  provider: { id: string; name: string; photoUrl: string | null; isVerified: boolean; averageRating: number | null } | null;
  status: string;
  description: string;
  postalCode: string;
  city: string;
  canton: string;
  preferredDate: string | null;
  preferredTimeSlot: string | null;
  budgetMin: number | null;
  budgetMax: number | null;
  agreedPrice: number | null;
  paymentStatus: string;
  rating: { score: number; comment: string } | null;
  unreadMessageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  senderId: string;
  senderName: string;
  content: string;
  isRead: boolean;
  createdAt: string;
}

// API functions
export const authApi = {
  register: async (data: {
    email: string;
    password: string;
    name: string;
    phone?: string;
    registerAsProvider?: boolean;
  }): Promise<AuthResponse> => {
    const response = await api.post('/api/auth/register', data);
    return response.data;
  },

  login: async (data: { email: string; password: string }): Promise<AuthResponse> => {
    const response = await api.post('/api/auth/login', data);
    return response.data;
  },

  me: async (): Promise<User> => {
    const response = await api.get('/api/auth/me');
    return response.data;
  },
};

export const categoryApi = {
  getAll: async (): Promise<Category[]> => {
    const response = await api.get('/api/categories');
    return response.data;
  },

  getBySlug: async (slug: string): Promise<Category> => {
    const response = await api.get(`/api/categories/${slug}`);
    return response.data;
  },
};

export const providerApi = {
  match: async (data: {
    categoryId: string;
    postalCode: string;
    city: string;
    preferredTime?: string;
  }): Promise<ProviderMatch[]> => {
    const response = await api.post('/api/providers/match', data);
    return response.data;
  },

  getById: async (id: string): Promise<any> => {
    const response = await api.get(`/api/providers/${id}`);
    return response.data;
  },

  getProfile: async (): Promise<any> => {
    const response = await api.get('/api/providers/profile');
    return response.data;
  },

  createProfile: async (data: {
    bio: string;
    categoryIds: number[];
    languages: string[];
    postalCode: string;
    city: string;
    serviceRadius: number;
    hourlyRate: number;
    certifications?: string[];
    yearsOfExperience?: number;
  }): Promise<any> => {
    const response = await api.post('/api/providers/profile', data);
    return response.data;
  },

  updateProfile: async (data: any): Promise<any> => {
    const response = await api.put('/api/providers/profile', data);
    return response.data;
  },

  getBookings: async (): Promise<Booking[]> => {
    const response = await api.get('/api/providers/bookings');
    return response.data;
  },
};

export const bookingApi = {
  create: async (data: {
    categoryId: string;
    providerId?: string;
    description: string;
    postalCode: string;
    city: string;
    canton?: string;
    preferredDate?: string;
    preferredTimeSlot?: string;
    budgetMin?: number;
    budgetMax?: number;
  }): Promise<Booking> => {
    const response = await api.post('/api/bookings', data);
    return response.data;
  },

  getById: async (id: string): Promise<Booking> => {
    const response = await api.get(`/api/bookings/${id}`);
    return response.data;
  },

  getClientBookings: async (): Promise<Booking[]> => {
    const response = await api.get('/api/bookings/client');
    return response.data;
  },

  getProviderBookings: async (): Promise<Booking[]> => {
    const response = await api.get('/api/bookings/provider');
    return response.data;
  },

  getPendingRequests: async (): Promise<Booking[]> => {
    const response = await api.get('/api/bookings/provider/pending');
    return response.data;
  },

  accept: async (id: string): Promise<Booking> => {
    const response = await api.post(`/api/bookings/${id}/accept`);
    return response.data;
  },

  decline: async (id: string, reason?: string): Promise<Booking> => {
    const response = await api.post(`/api/bookings/${id}/decline`, null, {
      params: { reason },
    });
    return response.data;
  },

  complete: async (id: string): Promise<Booking> => {
    const response = await api.post(`/api/bookings/${id}/complete`);
    return response.data;
  },

  cancel: async (id: string): Promise<Booking> => {
    const response = await api.post(`/api/bookings/${id}/cancel`);
    return response.data;
  },

  getMessages: async (id: string): Promise<Message[]> => {
    const response = await api.get(`/api/bookings/${id}/messages`);
    return response.data;
  },

  sendMessage: async (id: string, content: string): Promise<Message> => {
    const response = await api.post(`/api/bookings/${id}/messages`, { content });
    return response.data;
  },
};

export const adminApi = {
  getProviders: async (): Promise<any[]> => {
    const response = await api.get('/api/admin/providers');
    return response.data;
  },

  verifyProvider: async (id: string, verified: boolean, notes?: string): Promise<any> => {
    const response = await api.put(`/api/admin/providers/${id}/verify`, null, {
      params: { verified, notes },
    });
    return response.data;
  },

  getBookings: async (page = 0, size = 20): Promise<any> => {
    const response = await api.get('/api/admin/bookings', {
      params: { page, size },
    });
    return response.data;
  },
};

export default api;
