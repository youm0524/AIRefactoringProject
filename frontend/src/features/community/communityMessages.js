export const mergeCommunityMessage = (messages, incoming) => [
  ...messages.filter(message => message.id !== incoming.id),
  incoming
].sort((left, right) => new Date(left.createdAt) - new Date(right.createdAt) || left.id - right.id)

export const updateRoomWithMessage = (rooms, incoming) => rooms
  .map(room => room.id === incoming.roomId ? {...room, lastMessage: incoming} : room)
  .sort((left, right) => {
    if (!left.lastMessage && !right.lastMessage) return 0
    if (!left.lastMessage) return 1
    if (!right.lastMessage) return -1
    return new Date(right.lastMessage.createdAt) - new Date(left.lastMessage.createdAt)
      || right.lastMessage.id - left.lastMessage.id
  })
