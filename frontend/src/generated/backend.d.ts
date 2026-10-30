import type {WorkerRequest} from "../lib/messages";

export function onMessage(event: MessageEvent<WorkerRequest>): void;
