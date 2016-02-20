package com.ac.disruptor;


import com.lmax.disruptor.EventFactory;

class AddEvent {
    public void setSize(int size){
        this.size = size;
    }
    public int  getSize(){
        return size;
    }

    private int size;
}

class AddEventFactory implements EventFactory<AddEvent> {
    public AddEvent newInstance() {
        return new AddEvent();
    }
}
