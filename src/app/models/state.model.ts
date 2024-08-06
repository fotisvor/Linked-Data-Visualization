// src/app/models/state.model.ts

export class County {
    constructor(public name: string) {}
}

export class State {
    constructor(public name: string, public counties: County[]) {}
}
