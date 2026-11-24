// 인증 요청이 교체·취소된 뒤 도착한 응답은 현재 세션에 반영하지 않는다.
export const createSessionRequestScope = () => {
  let current = null
  return {
    start() {
      current?.abort()
      const controller = new AbortController()
      current = controller
      return {signal: controller.signal, isCurrent: () => current === controller && !controller.signal.aborted}
    },
    cancel() {
      current?.abort()
      current = null
    }
  }
}
