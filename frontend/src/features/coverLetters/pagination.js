export const getLastCoverLetterPage = (totalElements, size) =>
  Math.max(0, Math.ceil(totalElements / size) - 1)
