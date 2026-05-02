import client from './client'
import type { SubscriptionResponse, PlanInfo, SubscriptionPlan } from '../types/api'

export const getPlans = () => client.get<PlanInfo[]>('/subscriptions/plans')

export const getMySubscription = () => client.get<SubscriptionResponse>('/subscriptions/me')

export const createCheckout = (plan: SubscriptionPlan) =>
  client.post<{ checkoutUrl: string }>('/subscriptions/checkout', { plan })
