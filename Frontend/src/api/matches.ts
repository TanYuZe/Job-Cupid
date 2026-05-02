import client from './client'
import type { MatchResponse, PageResponse } from '../types/api'

export const getMatches = (page = 0, size = 20) =>
  client.get<PageResponse<MatchResponse>>('/matches', { params: { page, size } })

export const getMatch = (matchId: string) =>
  client.get<MatchResponse>(`/matches/${matchId}`)
