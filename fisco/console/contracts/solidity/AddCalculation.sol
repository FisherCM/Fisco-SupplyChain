pragma solidity ^0.4.24;

contract AddCalculation {
    int numA;
    int numB;

    function AddCalculation() {
        numA = 1;
        numB = 2;
    }

    function calculate()constant returns(int) {
        return numA + numB;
    }

    function set(int inputA, int inputB) {
        numA = inputA;
        numB = inputB;
    }
}
